package com.example.backend.admin.service;

import com.example.backend.admin.domain.Coupon;
import com.example.backend.admin.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminCouponService {
    private final CouponRepository couponRepository;

    public Page<Coupon> list(Boolean active, Coupon.DiscountType discountType, String code, String name, Pageable pageable) {
        return couponRepository.search(active, discountType, code, name, pageable);
    }

    public java.util.Map<String, Long> getStats() {
        Long total = couponRepository.count();
        Long active = couponRepository.countActiveCoupons();
        Long inactive = couponRepository.countInactiveCoupons();
        Long expired = couponRepository.countExpiredCoupons();

        return java.util.Map.of(
            "totalCoupons", total,
            "activeCoupons", active,
            "inactiveCoupons", inactive,
            "expiredCoupons", expired
        );
    }
    public Coupon get(Long id) { return couponRepository.findById(id).orElseThrow(); }
    public Coupon create(Coupon c) {
        if (couponRepository.existsByCode(c.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists");
        }
        return couponRepository.save(c);
    }

    // 필요한 필드만 업데이트하도록 수정
    public Coupon update(Long id, Coupon req) {
        Coupon c = couponRepository.findById(id).orElseThrow();
        if (req.getCode() != null && !req.getCode().equals(c.getCode())) {
            if (couponRepository.existsByCodeAndIdNot(req.getCode(), id)) {
                throw new IllegalArgumentException("Coupon code already exists");
            }
            c.setCode(req.getCode());
        }

        if (req.getName() != null) c.setName(req.getName());
        if (req.getDiscountType() != null) c.setDiscountType(req.getDiscountType());
        if (req.getDiscountValue() != null) c.setDiscountValue(req.getDiscountValue());
        if (req.getMinSpend() != null) c.setMinSpend(req.getMinSpend());
        if (req.getValidFrom() != null) c.setValidFrom(req.getValidFrom());
        if (req.getValidTo() != null) c.setValidTo(req.getValidTo());
        if (req.getIsActive() != null) c.setIsActive(req.getIsActive());

        return couponRepository.save(c);
    }

    // 상태 변경만을 위한 메서드 추가
    @Transactional
    public void updateStatus(Long id, boolean isActive) {
        Coupon coupon = couponRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        coupon.setIsActive(isActive);
        couponRepository.save(coupon);
    }

    public void delete(Long id) { couponRepository.deleteById(id); }

    @Transactional
    public List<Coupon> bulkIssue(Long userId, String baseCode, int count, Coupon.DiscountType type,
                                  int discountValue, int minSpend, LocalDateTime validFrom, LocalDateTime validTo, boolean active) {
        List<Coupon> created = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String code = baseCode + "-" + i;
            if (couponRepository.existsByCode(code)) {
                // skip duplicates to keep operation idempotent-ish
                continue;
            }
            Coupon c = new Coupon();
            c.setUserId(userId);
            c.setName(baseCode + " " + i);
            c.setCode(code);
            c.setDiscountType(type);
            c.setDiscountValue(discountValue);
            c.setMinSpend(minSpend);
            c.setValidFrom(validFrom);
            c.setValidTo(validTo);
            c.setIsActive(active);
            created.add(couponRepository.save(c));
        }
        return created;
    }
}