package com.example.backend.admin.controller;

import com.example.backend.admin.domain.Coupon;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.service.AdminCouponService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminCouponController {
    private final AdminCouponService couponService;

    @GetMapping
    public ApiResponse<PageResponse<Coupon>> list(@RequestParam(required = false) Boolean active,
                                                 @RequestParam(required = false) Coupon.DiscountType discountType,
                                                 @RequestParam(required = false) String code,
                                                 @RequestParam(required = false) String name,
                                                 Pageable pageable) {
        Page<Coupon> page = couponService.list(active, discountType, code, name, pageable);
        return ApiResponse.ok(PageResponse.of(page));
    }

    @GetMapping("/stats")
    public ApiResponse<java.util.Map<String, Long>> stats() {
        return ApiResponse.ok(couponService.getStats());
    }

    @GetMapping("/{id}")
    public ApiResponse<Coupon> get(@PathVariable Long id) {
        return ApiResponse.ok(couponService.get(id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Coupon>> create(@RequestBody Coupon coupon) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(couponService.create(coupon)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Coupon>> update(@PathVariable Long id, @RequestBody Coupon coupon) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(couponService.update(id, coupon)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> statusUpdate) {
        try {
            Object statusValue = statusUpdate.get("status");
            if (statusValue == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("'status' field is required."));
            }

            boolean isActive;
            if (statusValue instanceof Boolean) {
                isActive = (Boolean) statusValue;
            } else if (statusValue instanceof String) {
                String statusStr = ((String) statusValue).toUpperCase();
                if ("ACTIVE".equals(statusStr) || "TRUE".equals(statusStr)) {
                    isActive = true;
                } else if ("INACTIVE".equals(statusStr) || "FALSE".equals(statusStr)) {
                    isActive = false;
                } else {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Invalid status value. Use 'ACTIVE', 'INACTIVE', true, or false."));
                }
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Invalid type for 'status' field."));
            }

            couponService.updateStatus(id, isActive);
            return ResponseEntity.ok(ApiResponse.ok(null));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("쿠폰 상태 변경에 실패했습니다: " + e.getMessage()));
        }
    }


    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ApiResponse.ok(null);
    }

    public static record BulkIssueRequest(
            Long userId,
            String baseCode,
            Integer count,
            Coupon.DiscountType discountType,
            Integer discountValue,
            Integer minSpend,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validFrom,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validTo,
            Boolean active
    ) {}

    @PostMapping("/bulk-issue")
    public ApiResponse<List<Coupon>> bulkIssue(@RequestBody BulkIssueRequest req) {
        int count = req.count() != null ? req.count() : 0;
        if (count <= 0) {
            return ApiResponse.fail("count must be > 0");
        }
        List<Coupon> created = couponService.bulkIssue(
                req.userId(), req.baseCode(), count, req.discountType(),
                req.discountValue(), req.minSpend(), req.validFrom(), req.validTo(), Boolean.TRUE.equals(req.active())
        );
        return ApiResponse.ok(created);
    }

    public static record AssignRequest(Long targetUserId, java.util.List<Long> couponIds) {}

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<List<Coupon>>> assign(@RequestBody AssignRequest req) {
        try {
            if (req == null || req.targetUserId() == null || req.couponIds() == null || req.couponIds().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("targetUserId and couponIds are required"));
            }
            List<Coupon> created = couponService.assign(req.targetUserId(), req.couponIds());
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("쿠폰 할당에 실패했습니다: " + e.getMessage()));
        }
    }
}