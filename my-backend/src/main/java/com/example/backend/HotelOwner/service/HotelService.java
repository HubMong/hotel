package com.example.backend.HotelOwner.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelAmenity;
import com.example.backend.HotelOwner.domain.HotelImage;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.authlogin.domain.User;
import com.example.backend.HotelOwner.dto.DailySalesDto;
import com.example.backend.HotelOwner.dto.DashboardDto;
import com.example.backend.HotelOwner.dto.HotelDto;
import com.example.backend.HotelOwner.dto.RoomDto;
import com.example.backend.HotelOwner.dto.SalesChartRequestDto;
import com.example.backend.HotelOwner.repository.AmenityRepository;
import com.example.backend.HotelOwner.repository.HotelAmenityRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.payment.repository.PaymentRepository;
import com.example.backend.admin.repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service("ownerHotelService")
@RequiredArgsConstructor
@Transactional
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final AdminUserRepository userRepository; // admin.repository.UserRepository
    private final AmenityRepository amenityRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Hotel createHotel(HotelDto hotelDto, List<String> imageUrls, List<Long> amenityIds, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 사용자를 찾을 수 없습니다."));

        Hotel hotel = Hotel.builder()
                .owner(owner)
                .name(hotelDto.getName())
                .businessId(hotelDto.getBusinessId())
                .address(hotelDto.getAddress())
                .starRating(hotelDto.getStarRating())
                .description(hotelDto.getDescription())
                .country(hotelDto.getCountry())
                // status → approvalStatus
                .approvalStatus(Hotel.ApprovalStatus.APPROVED)
                .images(new ArrayList<>())
                .hotelAmenities(new ArrayList<>())
                .build();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<HotelImage> hotelImages = imageUrls.stream()
                    .map(url -> HotelImage.builder().hotel(hotel).url(url).build())
                    .collect(Collectors.toList());
            hotel.getImages().addAll(hotelImages);
        }

        Hotel savedHotel = hotelRepository.save(hotel);

        if (amenityIds != null && !amenityIds.isEmpty()) {
            List<Amenity> amenities = amenityRepository.findAllById(amenityIds);
            List<HotelAmenity> hotelAmenities = amenities.stream()
                    .map(amenity -> HotelAmenity.builder().hotel(savedHotel).amenity(amenity).build())
                    .collect(Collectors.toList());
            hotelAmenityRepository.saveAll(hotelAmenities);
        }

        return savedHotel;
    }

    @Transactional(readOnly = true)
    public List<Hotel> getHotelsByOwner(Long ownerId) {
        log.info("3. [HotelService] getHotelsByOwner 호출됨, ownerId: {}", ownerId);
        List<Hotel> hotels = hotelRepository.findByOwnerIdWithDetails(ownerId);
        log.info("   [HotelService] DB 조회 결과 (이미지 포함), {}개의 호텔을 찾음", hotels.size());

        hotels.forEach(hotel -> {
            if (hotel.getHotelAmenities() != null) {
                hotel.getHotelAmenities().size();
            }
        });

        log.info("   [HotelService] 편의시설 정보 로딩 완료");
        return hotels;
    }

    @Transactional(readOnly = true)
    public Hotel getHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다. id=" + id));
    }

    @Transactional
    public void deleteHotel(Long id) {
        log.info("3. [Service-삭제] deleteHotel 호출됨, 호텔 ID: {}", id);

        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> {
                log.error("   [Service-삭제] 삭제할 호텔이 존재하지 않음, ID: {}", id);
                return new IllegalArgumentException("호텔이 존재하지 않습니다. id=" + id);
            });

        // 객실 먼저 삭제
        List<Room> roomsToDelete = roomRepository.findByHotel(hotel);
        if (!roomsToDelete.isEmpty()) {
            log.info("   [Service-삭제] 호텔에 속한 {}개의 객실을 먼저 삭제합니다.", roomsToDelete.size());
            roomRepository.deleteAll(roomsToDelete);
        }

        // 연관 편의시설 삭제
        log.info("   [Service-삭제] 연결된 편의시설 정보 삭제 시작");
        hotelAmenityRepository.deleteByHotel_Id(hotel.getId());

        // 호텔 삭제
        log.info("   [Service-삭제] 호텔 본체 삭제 시작");
        hotelRepository.delete(hotel);

        log.info("   [Service-삭제] 호텔 ID {} 삭제 완료", id);
    }

    @Transactional
    public HotelDto updateHotel(Long id, HotelDto hotelDto, List<String> imageUrls, List<Long> amenityIds) {
        log.info("3. [Service-수정] updateHotel 호출됨, 호텔 ID: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다. id=" + id));
        log.info("   [Service-수정] 수정할 호텔을 찾음: {}", hotel.getName());

        hotel.setName(hotelDto.getName());
        hotel.setAddress(hotelDto.getAddress());
        hotel.setStarRating(hotelDto.getStarRating());
        hotel.setCountry(hotelDto.getCountry());
        hotel.setDescription(hotelDto.getDescription());
        hotel.setBusinessId(hotelDto.getBusinessId());
        log.info("   [Service-수정] 호텔 기본 정보 업데이트 완료");

        hotel.getImages().clear();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<HotelImage> newImages = imageUrls.stream()
                    .map(url -> HotelImage.builder().hotel(hotel).url(url).build())
                    .collect(Collectors.toList());
            hotel.getImages().addAll(newImages);
        }
        log.info("   [Service-수정] 이미지 정보 업데이트 완료. 새 이미지 수: {}", imageUrls != null ? imageUrls.size() : 0);

        hotel.getHotelAmenities().clear();
        log.info("   [Service-수정] 기존 편의시설 연결을 모두 제거했습니다.");

        if (amenityIds != null && !amenityIds.isEmpty()) {
            List<Amenity> amenities = amenityRepository.findAllById(amenityIds);
            List<HotelAmenity> newHotelAmenities = amenities.stream()
                .map(amenity -> HotelAmenity.builder().hotel(hotel).amenity(amenity).build())
                .collect(Collectors.toList());
            hotel.getHotelAmenities().addAll(newHotelAmenities);
            log.info("   [Service-수정] {}개의 새 편의시설 연결을 추가했습니다.", newHotelAmenities.size());
        }

        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("   [Service-수정] 호텔 저장 완료. 트랜잭션 커밋 대기 중...");

        return HotelDto.fromEntity(savedHotel);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> findRoomsByHotelId(Long hotelId) {
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return rooms.stream()
            .map(RoomDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDtos.OwnerReservationResponse> getReservationsByOwner(Long ownerId) {
        List<Reservation> reservations = reservationRepository.findAllByHotelOwnerId(ownerId);
        if (reservations.isEmpty()) return List.of();

        List<Long> userIds = reservations.stream().map(Reservation::getUserId).distinct().collect(Collectors.toList());
        List<Long> roomIds = reservations.stream().map(Reservation::getRoomId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, Room> roomMap = roomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, room -> room));

        return reservations.stream()
                .map(reservation -> {
                    User user = userMap.get(reservation.getUserId());
                    Room room = roomMap.get(reservation.getRoomId());
                    if (user == null || room == null) return null;
                    return ReservationDtos.OwnerReservationResponse.fromEntity(reservation, user, room);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public DashboardDto getSalesSummary(Long ownerId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        long todaySales = getSalesForDate(ownerId, today);
        long yesterdaySales = getSalesForDate(ownerId, yesterday);

        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        long thisWeekSales = getSalesForDateRange(ownerId, thisWeekStart, thisWeekEnd);

        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = thisWeekEnd.minusWeeks(1);
        long lastWeekSales = getSalesForDateRange(ownerId, lastWeekStart, lastWeekEnd);

        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        long thisMonthSales = getSalesForDateRange(ownerId, thisMonthStart, thisMonthEnd);

        LocalDate lastMonth = today.minusMonths(1);
        LocalDate lastMonthStart = lastMonth.withDayOfMonth(1);
        LocalDate lastMonthEnd = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
        long lastMonthSales = getSalesForDateRange(ownerId, lastMonthStart, lastMonthEnd);

        return DashboardDto.builder()
                .todaySales(todaySales)
                .thisWeekSales(thisWeekSales)
                .thisMonthSales(thisMonthSales)
                .salesChangeVsYesterday(calculateChangePercentage(todaySales, yesterdaySales))
                .salesChangeVsLastWeek(calculateChangePercentage(thisWeekSales, lastWeekSales))
                .salesChangeVsLastMonth(calculateChangePercentage(thisMonthSales, lastMonthSales))
                .build();
    }

    private long getSalesForDate(Long ownerId, LocalDate date) {
        Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return paymentRepository.sumCompletedPaymentsByOwnerAndDateRange(ownerId, start, end, Instant.now());
    }

    private long getSalesForDateRange(Long ownerId, LocalDate startDate, LocalDate endDate) {
        Instant start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return paymentRepository.sumCompletedPaymentsByOwnerAndDateRange(ownerId, start, end, Instant.now());
    }

    private double calculateChangePercentage(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((double) (current - previous) / previous) * 100;
    }

    @Transactional(readOnly = true)
    public List<DailySalesDto> getDailySales(Long ownerId, SalesChartRequestDto requestDto) {
        Instant start = requestDto.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = requestDto.getEndDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return paymentRepository.findDailySalesByOwner(
            ownerId, start, end, requestDto.getHotelId(), requestDto.getRoomType()
        );
    }

    public ReservationDtos.DashboardActivityResponse getDashboardActivity(Long ownerId) {
        LocalDate today = LocalDate.now();

        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<Reservation> checkIns = reservationRepository.findCheckInsForOwnerByDateRange(ownerId, startOfDay, endOfDay);
        List<Reservation> checkOuts = reservationRepository.findCheckOutsForOwnerByDateRange(ownerId, startOfDay, endOfDay);

        // ← 여기 Instant로 통일
        Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
        List<Reservation> recentReservations =
            reservationRepository.findRecentReservationsForOwner(ownerId, threeDaysAgo, PageRequest.of(0, 5));

        List<Reservation> allReservations = Stream.of(checkIns, checkOuts, recentReservations)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        if (allReservations.isEmpty()) {
            return new ReservationDtos.DashboardActivityResponse(List.of(), List.of(), List.of());
        }

        List<Long> userIds = allReservations.stream().map(Reservation::getUserId).distinct().collect(Collectors.toList());
        List<Long> roomIds = allReservations.stream().map(Reservation::getRoomId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, Room> roomMap = roomRepository.findAllById(roomIds).stream()
                .collect(Collectors.toMap(Room::getId, room -> room));

        Function<Reservation, ReservationDtos.OwnerReservationResponse> toDto = reservation -> {
            User user = userMap.get(reservation.getUserId());
            Room room = roomMap.get(reservation.getRoomId());
            if (user == null || room == null) return null;
            return ReservationDtos.OwnerReservationResponse.fromEntity(reservation, user, room);
        };

        List<ReservationDtos.OwnerReservationResponse> checkInDtos = checkIns.stream().map(toDto).filter(dto -> dto != null).toList();
        List<ReservationDtos.OwnerReservationResponse> checkOutDtos = checkOuts.stream().map(toDto).filter(dto -> dto != null).toList();
        List<ReservationDtos.OwnerReservationResponse> recentReservationDtos = recentReservations.stream().map(toDto).filter(dto -> dto != null).toList();

        return new ReservationDtos.DashboardActivityResponse(checkInDtos, checkOutDtos, recentReservationDtos);
    }
}
