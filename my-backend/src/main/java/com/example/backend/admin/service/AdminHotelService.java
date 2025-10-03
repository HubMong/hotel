package com.example.backend.admin.service;

import com.example.backend.admin.dto.HotelAdminDto;
import com.example.backend.authlogin.domain.User;
import com.example.backend.admin.repository.AdminUserRepository;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.Comparator;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminHotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final AdminUserRepository userRepository;

    public Page<HotelAdminDto> list(String name, Integer minStar, Hotel.ApprovalStatus status, Pageable pageable) {
        Pageable safePageable = pageable != null ? pageable : PageRequest.of(0, 20);

        Page<Hotel> hotels;
        if (status != null) {
            hotels = hotelRepository.searchByApproval(status.name(), name, minStar, safePageable);
        } else if (name != null || minStar != null) {
            hotels = hotelRepository.search(name, minStar, safePageable);
        } else {
            hotels = hotelRepository.findAll(safePageable);
        }

        List<HotelAdminDto> content = hotels.stream()
            .map(this::mapToSimpleHotelAdminDto)
            .toList();

        return new PageImpl<>(content, safePageable, hotels.getTotalElements());
    }

    public Page<HotelAdminDto> listWithBusinessInfo(String name, Integer minStar, Hotel.ApprovalStatus status, Pageable pageable) {
        Pageable safePageable = pageable != null ? pageable : PageRequest.of(0, 20);

        List<Object[]> rows = hotelRepository.findHotelsWithBusinessInfo(
            status != null ? status.name() : null,
            name,
            minStar
        );

        List<HotelAdminDto> allDtos = rows.stream()
            .map(this::mapToAggregatedDto)
            .toList();

        // Apply sorting from pageable (frontend passes sort=prop,asc|desc)
        Sort sort = safePageable.getSort();
        if (sort != null && sort.isSorted()) {
            Comparator<HotelAdminDto> comparator = null;
            for (Sort.Order order : sort) {
                Comparator<HotelAdminDto> c = comparatorFor(order);
                if (c == null) continue; // skip unknown properties
                comparator = (comparator == null) ? c : comparator.thenComparing(c);
            }
            if (comparator != null) {
                allDtos = allDtos.stream().sorted(comparator).toList();
            }
        }

        int start = (int) safePageable.getOffset();
        int end = Math.min(start + safePageable.getPageSize(), allDtos.size());
        List<HotelAdminDto> pageContent = start >= allDtos.size() ? Collections.emptyList() : allDtos.subList(start, end);

        return new PageImpl<>(pageContent, safePageable, allDtos.size());
    }

    private Comparator<HotelAdminDto> comparatorFor(Sort.Order order) {
        String prop = order.getProperty();
        boolean asc = order.getDirection().isAscending();
        Comparator<HotelAdminDto> base;
        // Only allow known properties to avoid unexpected sorting by unsupported fields
        switch (prop) {
            case "id" -> base = Comparator.comparing(HotelAdminDto::id, Comparator.nullsLast(Comparator.naturalOrder()));
            case "status" -> base = Comparator.comparing(HotelAdminDto::status, Comparator.nullsLast(Comparator.naturalOrder()));
            case "businessName" -> base = Comparator.comparing(HotelAdminDto::businessName, Comparator.nullsLast(Comparator.naturalOrder()));
            case "businessEmail" -> base = Comparator.comparing(HotelAdminDto::businessEmail, Comparator.nullsLast(Comparator.naturalOrder()));
            case "businessPhone" -> base = Comparator.comparing(HotelAdminDto::businessPhone, Comparator.nullsLast(Comparator.naturalOrder()));
            case "businessNumber" -> base = Comparator.comparing(HotelAdminDto::businessNumber, Comparator.nullsLast(Comparator.naturalOrder()));
            case "address" -> base = Comparator.comparing(HotelAdminDto::address, Comparator.nullsLast(Comparator.naturalOrder()));
            case "createdAt" -> base = Comparator.comparing(HotelAdminDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> {
                return null; // unsupported property
            }
        }
        return asc ? base : base.reversed();
    }

    public HotelAdminDto get(Long id) {
        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + id));

        Object[] stats = hotelRepository.findHotelStats(id);
        User businessUser = fetchBusinessUser(hotel.getUserId());
        List<Room> rooms = roomRepository.findByHotelId(id);

        return HotelAdminDto.from(
            hotel,
            businessUser != null ? businessUser.getName() : null,
            businessUser != null ? businessUser.getEmail() : null,
            businessUser != null ? businessUser.getPhone() : null,
            businessNumberOf(hotel),
            stats != null ? intValue(stats, 0) : 0,
            stats != null ? intValue(stats, 1) : 0,
            stats != null ? doubleValue(stats, 2) : 0.0,
            stats != null ? longValue(stats, 3) : 0L,
            rooms
        );
    }

    @Transactional
    public void approve(Long id, Long adminUserId, String note) {
        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + id));

        if (hotel.getApprovalStatus() == Hotel.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("이미 승인된 호텔입니다.");
        }

        hotel.setApprovalStatus(Hotel.ApprovalStatus.APPROVED);
        hotel.setApprovalDate(LocalDateTime.now());
        hotel.setApprovedBy(adminUserId);
        hotel.setRejectionReason(note);
        hotelRepository.save(hotel);
    }

    @Transactional
    public void reject(Long id, String reason) {
        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + id));

        hotel.setApprovalStatus(Hotel.ApprovalStatus.REJECTED);
        hotel.setApprovalDate(LocalDateTime.now());
        hotel.setRejectionReason(reason);
        hotelRepository.save(hotel);
    }

    @Transactional
    public void suspend(Long id, String reason) {
        Hotel hotel = hotelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + id));

        hotel.setApprovalStatus(Hotel.ApprovalStatus.SUSPENDED);
        hotel.setRejectionReason(reason);
        hotelRepository.save(hotel);
    }

    @Transactional
    public void delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new IllegalArgumentException("호텔을 찾을 수 없습니다: " + id);
        }
        hotelRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Room> listRoomsByHotel(Long hotelId) {
        if (hotelId == null) {
            throw new IllegalArgumentException("hotelId는 필수입니다.");
        }
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        
        // Lazy loading 강제 초기화
        for (Room room : rooms) {
            room.getImages().size(); // Force initialization
        }
        
        return rooms;
    }

    private HotelAdminDto mapToSimpleHotelAdminDto(Hotel hotel) {
        User businessUser = fetchBusinessUser(hotel.getUserId());

        return HotelAdminDto.from(
            hotel,
            businessUser != null ? businessUser.getName() : null,
            businessUser != null ? businessUser.getEmail() : null,
            businessUser != null ? businessUser.getPhone() : null,
            businessNumberOf(hotel),
            0,
            0,
            0.0,
            0L
        );
    }

    private HotelAdminDto mapToAggregatedDto(Object[] row) {
        if (row == null || row.length < 18) {
            return fallbackDto(row);
        }

        Long hotelId = longValue(row, 0, null);
        if (hotelId == null) {
            return fallbackDto(row);
        }

        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + hotelId));
        User businessUser = fetchBusinessUser(hotel.getUserId());

        return HotelAdminDto.from(
            hotel,
            businessUser != null ? businessUser.getName() : null,
            businessUser != null ? businessUser.getEmail() : null,
            businessUser != null ? businessUser.getPhone() : null,
            businessNumberOf(hotel),
            intValue(row, 14),
            intValue(row, 15),
            doubleValue(row, 16),
            longValue(row, 17)
        );
    }

    private HotelAdminDto fallbackDto(Object[] row) {
        Long hotelId = row != null ? longValue(row, 0, null) : null;
        if (hotelId == null) {
            throw new IllegalArgumentException("호텔 정보를 확인할 수 없습니다.");
        }
        Hotel hotel = hotelRepository.findById(hotelId)
            .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + hotelId));
        return mapToSimpleHotelAdminDto(hotel);
    }

    private User fetchBusinessUser(Long userId) {
        if (userId == null) {
            return null;
        }
        Optional<User> user = userRepository.findById(userId);
        return user.orElse(null);
    }

    private String businessNumberOf(Hotel hotel) {
        if (hotel == null || hotel.getBusinessId() == null) {
            return null;
        }
        return String.valueOf(hotel.getBusinessId());
    }

    private int intValue(Object[] row, int index) {
        if (row == null || index >= row.length) {
            return 0;
        }
        return intValue(row[index]);
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return (int) Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private long longValue(Object[] row, int index) {
        if (row == null || index >= row.length) {
            return 0L;
        }
        return longValue(row[index], 0L);
    }

    private Long longValue(Object[] row, int index, Long defaultValue) {
        if (row == null || index >= row.length) {
            return defaultValue;
        }
        return longValue(row[index], defaultValue);
    }

    // removed unused overload longValue(Object)

    private Long longValue(Object value, Long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Math.round(Double.parseDouble(str));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private double doubleValue(Object[] row, int index) {
        if (row == null || index >= row.length) {
            return 0.0;
        }
        return doubleValue(row[index]);
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }

    public java.util.Map<String, Long> getStatusCounts() {
        long pending = hotelRepository.countByApprovalStatus(Hotel.ApprovalStatus.PENDING);
        long approved = hotelRepository.countByApprovalStatus(Hotel.ApprovalStatus.APPROVED);
        long rejected = hotelRepository.countByApprovalStatus(Hotel.ApprovalStatus.REJECTED);
        long suspended = hotelRepository.countByApprovalStatus(Hotel.ApprovalStatus.SUSPENDED);

        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        counts.put("PENDING", pending);
        counts.put("APPROVED", approved);
        counts.put("REJECTED", rejected);
        counts.put("SUSPENDED", suspended);

        return counts;
    }
}
