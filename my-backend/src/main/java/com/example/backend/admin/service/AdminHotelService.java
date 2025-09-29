package com.example.backend.admin.service;

import com.example.backend.admin.dto.HotelAdminDto;
import com.example.backend.admin.repository.UserRepository; // 필요 없지만 남겨둬도 무방
import com.example.backend.authlogin.domain.User;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminHotelService {
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;   // 직접 사용 안 해도 주입 유지 가능
    private final RoomRepository roomRepository;   // 객실 상세용

    /** 간단 목록: 승인상태/이름/별점 필터 (레포 커스텀 쿼리 제거, 메모리 필터 + 수동 페이징) */
    public Page<HotelAdminDto> list(String name, Integer minStar, Hotel.ApprovalStatus status, Pageable pageable) {
        List<Hotel> all = hotelRepository.findAll();

        String q = name != null ? name.trim().toLowerCase(Locale.ROOT) : null;

        List<Hotel> filtered = all.stream()
                .filter(h -> status == null || Objects.equals(h.getApprovalStatus(), status))
                .filter(h -> q == null || (h.getName() != null && h.getName().toLowerCase(Locale.ROOT).contains(q)))
                .filter(h -> minStar == null || (h.getStarRating() != null && h.getStarRating() >= minStar))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Hotel> pageSlice = start > end ? List.of() : filtered.subList(start, end);

        List<HotelAdminDto> dtos = pageSlice.stream()
                .map(this::mapToSimpleHotelAdminDto)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, filtered.size());
    }

    /** 목록용 간단 DTO 매핑(사업자 정보만 포함, 통계는 0으로) */
    private HotelAdminDto mapToSimpleHotelAdminDto(Hotel hotel) {
        String businessName = "N/A";
        String businessEmail = "N/A";
        String businessPhone = "N/A";

        // Owner 엔티티 기준: owner(User)에서 사업자 정보 추출
        User owner = hotel.getOwner();
        if (owner != null) {
            if (owner.getName() != null)  businessName = owner.getName();
            if (owner.getEmail() != null) businessEmail = owner.getEmail();
            if (owner.getPhone() != null) businessPhone = owner.getPhone();
        }

        return HotelAdminDto.from(
                hotel,
                businessName,
                businessEmail,
                businessPhone,
                0,    // room_count
                0,    // reservation_count
                0.0,  // average_rating
                0L    // total_revenue
        );
    }

    /** (선택) 과거 findHotelsWithBusinessInfo 대체: 지금은 list() 결과와 동일 로직으로 제공 */
    public Page<HotelAdminDto> listWithBusinessInfo(String name, Integer minStar, Hotel.ApprovalStatus status, Pageable pageable) {
        return list(name, minStar, status, pageable);
    }

    /** 상세 조회: 조인 쿼리 제거, 단건 + 객실 목록 + owner에서 사업자 정보 추출 */
    public HotelAdminDto get(Long id) {
        Hotel hotel = hotelRepository.findById(id).orElseThrow();
        List<Room> rooms = roomRepository.findByHotelId(id);

        User owner = hotel.getOwner();
        String businessName = owner != null && owner.getName()  != null ? owner.getName()  : "N/A";
        String businessEmail= owner != null && owner.getEmail() != null ? owner.getEmail() : "N/A";
        String businessPhone= owner != null && owner.getPhone() != null ? owner.getPhone() : "N/A";

        return HotelAdminDto.from(
                hotel,
                businessName,
                businessEmail,
                businessPhone,
                0,    // total_rooms (통계 미계산)
                0,    // total_reservations
                0.0,  // average_rating
                0L,   // total_revenue
                rooms
        );
    }

    public void delete(Long id) {
        hotelRepository.deleteById(id);
    }

    public void approve(Long id, Long adminUserId, String note) {
        Hotel h = hotelRepository.findById(id).orElseThrow();
        if (h.getApprovalStatus() == Hotel.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("이미 승인된 호텔입니다.");
        }
        h.setApprovalStatus(Hotel.ApprovalStatus.APPROVED);
        h.setApprovalDate(java.time.LocalDateTime.now());
        h.setApprovedBy(adminUserId); // null 허용
        h.setRejectionReason(null);
        hotelRepository.save(h);
    }

    public void reject(Long id, String reason) {
        Hotel h = hotelRepository.findById(id).orElseThrow();
        h.setApprovalStatus(Hotel.ApprovalStatus.REJECTED);
        h.setRejectionReason(reason);
        hotelRepository.save(h);
    }

    public void suspend(Long id, String reason) {
        Hotel h = hotelRepository.findById(id).orElseThrow();
        h.setApprovalStatus(Hotel.ApprovalStatus.SUSPENDED);
        h.setRejectionReason(reason);
        hotelRepository.save(h);
    }
}
