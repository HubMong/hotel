package com.example.backend.admin.repository;

import com.example.backend.admin.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);

    @Query("SELECT c FROM Coupon c WHERE (:active IS NULL OR c.isActive = :active) " +
           "AND (:discountType IS NULL OR c.discountType = :discountType) " +
           "AND (:code IS NULL OR c.code LIKE CONCAT('%', :code, '%')) " +
           "AND (:name IS NULL OR c.name LIKE CONCAT('%', :name, '%'))")
    Page<Coupon> search(@Param("active") Boolean active,
                       @Param("discountType") Coupon.DiscountType discountType,
                       @Param("code") String code,
                       @Param("name") String name,
                       Pageable pageable);

    // 쿠폰 통계 조회
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.isActive = true")
    Long countActiveCoupons();

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.isActive = false")
    Long countInactiveCoupons();

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.validTo < CURRENT_TIMESTAMP")
    Long countExpiredCoupons();

    // 사용자 보유 쿠폰 수 (현재 활성/유효기간 내)
    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.userId = :userId AND c.isActive = true AND (c.validTo IS NULL OR c.validTo > CURRENT_TIMESTAMP)")
    Long countActiveCouponsByUserId(@Param("userId") Long userId);

    // 사용자별 쿠폰 목록 조회 (최신순)
    @Query("SELECT c FROM Coupon c WHERE c.userId = :userId ORDER BY c.id DESC")
    java.util.List<Coupon> findByUserId(@Param("userId") Long userId);
}
