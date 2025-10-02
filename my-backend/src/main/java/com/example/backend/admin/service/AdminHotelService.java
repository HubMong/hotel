package com.example.backend.admin.service;

import com.example.backend.admin.dto.HotelAdminDto;
import com.example.backend.admin.repository.UserRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException; // ✅ 단건 조회 예외

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminHotelService {
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;   // 현재 list()에선 사용 안 하지만 유지
    private final RoomRepository roomRepository;

    /**
     * 변경점:
     * 1) @Transactional(readOnly = true) 로 조회 중 영속성 컨텍스트 유지 → LAZY 안전
     * 2) Specification + repository.findAll(spec, pageable) 로 DB에서 필터/정렬/페이징 수행
     * 3) (가정) Repository에 @EntityGraph(owner) 적용된 findAll이 있다면 owner 즉시 로딩
     */
    @Transactional(readOnly = true)
    public Page<HotelAdminDto> list(String name, Integer minStar, Hotel.ApprovalStatus status, Pageable pageable) {
        Specification<Hotel> spec = Specification.where(null);

        if (status != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("approvalStatus"), status));
        }
        if (name != null && !name.isBlank()) {
            String q = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, cq, cb) -> cb.like(cb.lower(root.get("name")), q));
        }
        if (minStar != null) {
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("starRating"), minStar));
        }

        Page<Hotel> page = hotelRepository.findAll(spec, pageable); // DB 정렬/페이징/필터
        return page.map(this::mapToSimpleHotelAdminDto);
    }

    /** 목록용 간단 DTO 매핑(사업자 정보만 포함, 통계는 0으로) — 기존 그대로 사용 */
    private HotelAdminDto mapToSimpleHotelAdminDto(Hotel hotel) {
        String businessName  = "N/A";
        String businessEmail = "N/A";
        String businessPhone = "N/A";

        User owner = hotel.getOwner(); // (findAll에 @EntityGraph가 없다면 list()는 readOnly 트랜잭션 내라 LAZY 안전)
        if (owner != null) {
            if (owner.getName()  != null) businessName  = owner.getName();
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

    @Transactional
    public void delete(Long id) {
        hotelRepository.deleteById(id);
    }

    @Transactional
    public void approve(Long id, Long adminUserId, String note) {
        Hotel h = hotelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hotel not found: " + id));
        if (h.getApprovalStatus() == Hotel.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("이미 승인된 호텔입니다.");
        }
        h.setApprovalStatus(Hotel.ApprovalStatus.APPROVED);
        h.setApprovalDate(java.time.LocalDateTime.now());
        h.setApprovedBy(adminUserId); // null 허용
        h.setRejectionReason(null);
        // note 저장 컬럼이 따로 없다면 무시
        hotelRepository.save(h);
    }

    @Transactional
    public void reject(Long id, String reason) {
        Hotel h = hotelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hotel not found: " + id));
        h.setApprovalStatus(Hotel.ApprovalStatus.REJECTED);
        h.setRejectionReason(reason);
        hotelRepository.save(h);
    }

    @Transactional
    public void suspend(Long id, String reason) {
        Hotel h = hotelRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Hotel not found: " + id));
        h.setApprovalStatus(Hotel.ApprovalStatus.SUSPENDED);
        h.setRejectionReason(reason);
        hotelRepository.save(h);
    }

    @Transactional(readOnly = true)
    public HotelAdminDto get(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found: " + id));

        return mapToSimpleHotelAdminDto(hotel);
    }

    /** (옵션) 컨트롤러가 엔티티를 필요로 하면 이 메서드를 사용 */
    @Transactional(readOnly = true)
    public Hotel getEntity(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found: " + id));
    }
}
