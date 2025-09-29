package com.example.backend.HotelOwner.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.HotelOwner.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    // 오너별 호텔 (이미지까지 fetch)
    @Query("SELECT DISTINCT h FROM Hotel h LEFT JOIN FETCH h.images WHERE h.owner.id = :ownerId")
    List<Hotel> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    // ★ 권한체크: 호텔 id + 오너 email
    boolean existsByIdAndOwner_Email(Long id, String email);

    // 사업자 등록번호로 조회 (비즈니스 컨트롤러에서 사용)
    Optional<Hotel> findByBusinessId(Long businessId);
}
