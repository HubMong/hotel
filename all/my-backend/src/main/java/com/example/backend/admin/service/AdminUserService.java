package com.example.backend.admin.service;

import com.example.backend.admin.dto.UserAdminDto;
import com.example.backend.admin.dto.UserStatsDto;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.payment.repository.PaymentRepository;
import com.example.backend.admin.repository.CouponRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.admin.repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final AdminUserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final CouponRepository couponRepository;

    public Page<UserAdminDto> list(String name,
                                   String email,
                                   User.Role role,
                                   String activityStatus,
                                   Integer dormantMonths,
                                   User.Provider provider,
                                   String hasReservation,
                                   String joinPeriod,
                                   Pageable pageable) {
        java.time.LocalDateTime activeSince = null;
        if (activityStatus != null && !activityStatus.isBlank()) {
            int months = (dormantMonths != null && dormantMonths > 0) ? dormantMonths : 12;
            activeSince = java.time.LocalDateTime.now().minusMonths(months);
        }

        // 가입 기간 해석
        java.time.LocalDateTime joinFrom = null;
        if (joinPeriod != null && !joinPeriod.isBlank()) {
            var now = java.time.LocalDateTime.now();
            switch (joinPeriod) {
                case "today": joinFrom = now.toLocalDate().atStartOfDay(); break;
                case "week": joinFrom = now.minusWeeks(1); break;
                case "month": joinFrom = now.minusMonths(1); break;
                case "quarter": joinFrom = now.minusMonths(3); break;
                case "year": joinFrom = now.minusYears(1); break;
            }
        }

    return userRepository.findUsersWithAdvancedFilters(
        name, email, role, activityStatus, activeSince, provider, hasReservation, joinFrom, User.Role.ADMIN, pageable)
                .map(UserAdminDto::from);
    }

    public UserAdminDto get(Long id) {
        return UserAdminDto.from(userRepository.findById(id).orElseThrow());
    }

    public com.example.backend.admin.dto.UserStatsDto userSummary(Long userId) {
        userRepository.findById(userId).orElseThrow();
        long totalReservations = reservationRepository.countByUserId(userId);
        Long completedSum = paymentRepository.sumTotalCompletedByUserId(userId);
        long totalPayment = completedSum != null ? completedSum : 0L;
        long totalCoupons = java.util.Optional.ofNullable(couponRepository.countActiveCouponsByUserId(userId)).orElse(0L);
        
        // 최근 예약 정보
        var recent = reservationRepository.findRecentByUserId(userId).stream().map(r -> {
            var map = new java.util.HashMap<String,Object>();
            map.put("reservationId", ((Number) r[0]).longValue());
            map.put("hotelName", String.valueOf(r[1]));
            map.put("startDate", String.valueOf(r[2]));
            map.put("endDate", String.valueOf(r[3]));
            map.put("status", String.valueOf(r[4]));
            return map;
        }).toList();
        
        // 보유 쿠폰 정보
        var coupons = couponRepository.findByUserId(userId).stream().map(c -> {
            var map = new java.util.HashMap<String,Object>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("code", c.getCode());
            map.put("discountType", c.getDiscountType().name());
            map.put("discountValue", c.getDiscountValue());
            map.put("minSpend", c.getMinSpend());
            map.put("validFrom", c.getValidFrom().toString());
            map.put("validTo", c.getValidTo() != null ? c.getValidTo().toString() : null);
            map.put("isActive", c.getIsActive());
            // 유효성 검사
            boolean isValid = c.getIsActive() && 
                            (c.getValidTo() == null || c.getValidTo().isAfter(java.time.LocalDateTime.now()));
            map.put("isValid", isValid);
            return map;
        }).toList();
        
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String,Object>> recentCasted = (java.util.List<java.util.Map<String,Object>>) (java.util.List<?>) recent;
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String,Object>> couponsCasted = (java.util.List<java.util.Map<String,Object>>) (java.util.List<?>) coupons;
        
        return new UserStatsDto(totalReservations, totalPayment, totalCoupons, recentCasted, couponsCasted);
    }

    public void updateRole(Long id, User.Role role) {
        User u = userRepository.findById(id).orElseThrow();
        u.setRole(role);
        userRepository.save(u);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public void updateStatus(Long id, boolean active) {
        var user = userRepository.findById(id).orElseThrow();
        user.setActive(active);
        userRepository.save(user);
    }
}
