package com.example.backend.HotelOwner.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import com.example.backend.HotelOwner.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {

    // 오너별 호텔 (이미지까지 fetch)
    @Query("SELECT DISTINCT h FROM Hotel h LEFT JOIN FETCH h.images WHERE h.owner.id = :ownerId")
    List<Hotel> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    // ★ 권한체크: 호텔 id + 오너 email
    boolean existsByIdAndOwner_Email(Long id, String email);

    // 사업자 등록번호로 조회
    Optional<Hotel> findByBusinessId(Long businessId);

    // [ADDED] 관리자 목록용: owner를 즉시 로딩해서 LAZY 예외 방지 + DB 페이징/정렬
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Hotel> findAll(@Nullable Specification<Hotel> spec, Pageable pageable);
}
