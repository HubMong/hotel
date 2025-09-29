// src/main/java/com/example/backend/admin/controller/AdminDashboardController.java
package com.example.backend.admin.controller;

import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.DashboardDetailDto;
import com.example.backend.admin.dto.DashboardDto;
import com.example.backend.admin.repository.AdminPaymentRepository;
import com.example.backend.admin.repository.AdminReservationRepository;
import com.example.backend.admin.repository.CouponRepository;
import com.example.backend.admin.repository.InquiryRepository;
import com.example.backend.admin.repository.ReviewRepository;
import com.example.backend.admin.repository.UserRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final AdminReservationRepository reservationRepository;
    private final AdminPaymentRepository paymentRepository; // 관리자 전용 레포
    private final InquiryRepository inquiryRepository;
    private final ReviewRepository reviewRepository;
    private final CouponRepository couponRepository;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardDto>> summary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);

        // Reservation.createdAt(Instant) 집계용 파라미터
        Instant nowI = now.atZone(ZoneId.systemDefault()).toInstant();
        Instant startOfDayI = startOfDay.atZone(ZoneId.systemDefault()).toInstant();

        // 매출 합계: 레포 헬퍼(상태=PAID 고정) 사용
        Long recentRevenue = paymentRepository.sumPaidTotalPriceByCreatedAtBetween(
                now.minusDays(7), now);
        Long allRevenue    = paymentRepository.sumPaidTotalPriceByCreatedAtBetween(
                now.minusYears(50), now);

        DashboardDto dto = DashboardDto.builder()
                .totalUsers(userRepository.count())
                .totalBusinesses(userRepository.countByRole(com.example.backend.authlogin.domain.User.Role.BUSINESS))
                .totalHotels(hotelRepository.count())
                .totalReservations(reservationRepository.count())
                .totalPayments(paymentRepository.count())
                .totalReviews(reviewRepository.count())
                .totalCoupons(couponRepository.count())
                .pendingInquiries(inquiryRepository.countByStatus(com.example.backend.admin.domain.Inquiry.Status.PENDING))
                .recentRevenue(recentRevenue != null ? recentRevenue : 0L)
                .todayReservations(reservationRepository.countByCreatedAtBetween(startOfDayI, nowI))
                .totalRevenue(allRevenue != null ? allRevenue : 0L)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);

        Instant nowI = now.atZone(ZoneId.systemDefault()).toInstant();
        Instant startOfDayI = startOfDay.atZone(ZoneId.systemDefault()).toInstant();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalBusinesses", userRepository.countByRole(com.example.backend.authlogin.domain.User.Role.BUSINESS));
        stats.put("totalHotels", hotelRepository.count());
        stats.put("totalReservations", reservationRepository.count());
        stats.put("totalPayments", paymentRepository.count());
        stats.put("totalReviews", reviewRepository.count());
        stats.put("totalCoupons", couponRepository.count());
        stats.put("pendingInquiries", inquiryRepository.countByStatus(com.example.backend.admin.domain.Inquiry.Status.PENDING));
        stats.put("todayReservations", reservationRepository.countByCreatedAtBetween(startOfDayI, nowI));

        Long recentRevenue = paymentRepository.sumPaidTotalPriceByCreatedAtBetween(
                now.minusDays(7), now);
        Long allRevenue    = paymentRepository.sumPaidTotalPriceByCreatedAtBetween(
                now.minusYears(50), now);
        stats.put("recentRevenue", recentRevenue != null ? recentRevenue : 0L);
        stats.put("totalRevenue", allRevenue != null ? allRevenue : 0L);

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/details")
    public ResponseEntity<ApiResponse<DashboardDetailDto>> details(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer top,
            @RequestParam(required = false) Integer year) {

        int periodDays = (days == null || days < 1 || days > 60) ? 14 : days;
        int topLimit   = (top == null  || top < 1  || top > 50)  ? 5  : top;
        int targetYear = (year == null) ? LocalDate.now().getYear() : year;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(periodDays - 1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // 일별 매출(레포에서 PAID만), 일별 가입자
        var revenueRows = paymentRepository.dailyRevenue(from, now);
        var signupRows  = userRepository.countDailyUsers(from, now);

        var dailyRevenue = new ArrayList<DashboardDetailDto.DailyPoint>();
        var dailySignup  = new ArrayList<DashboardDetailDto.DailyPoint>();

        var revenueMap = new HashMap<String, Long>();
        for (Object[] row : revenueRows) {
            java.sql.Date d = (java.sql.Date) row[0];
            Number rev = (Number) row[1];
            revenueMap.put(d.toLocalDate().toString(), rev != null ? rev.longValue() : 0L);
        }
        var signupMap = new HashMap<String, Long>();
        for (Object[] row : signupRows) {
            java.sql.Date d = (java.sql.Date) row[0];
            Number cnt = (Number) row[1];
            signupMap.put(d.toLocalDate().toString(), cnt != null ? cnt.longValue() : 0L);
        }

        for (int i = 0; i < periodDays; i++) {
            var date = from.plusDays(i).toLocalDate();
            String key = date.toString();
            dailyRevenue.add(DashboardDetailDto.DailyPoint.builder()
                    .date(key).value(revenueMap.getOrDefault(key, 0L)).build());
            dailySignup.add(DashboardDetailDto.DailyPoint.builder()
                    .date(key).value(signupMap.getOrDefault(key, 0L)).build());
        }

        // 월별 가입
        var monthlyRows = userRepository.countMonthlyUsers(targetYear);
        var monthly = new ArrayList<DashboardDetailDto.MonthlyPoint>();
        int[] monthlyCounts = new int[13];
        for (Object[] r : monthlyRows) {
            Number m = (Number) r[0];
            Number c = (Number) r[1];
            int mi = m != null ? m.intValue() : 0;
            if (mi >= 1 && mi <= 12) monthlyCounts[mi] = c != null ? c.intValue() : 0;
        }
        for (int m = 1; m <= 12; m++) {
            monthly.add(DashboardDetailDto.MonthlyPoint.builder().month(m).count(monthlyCounts[m]).build());
        }

        // 호텔 매출 TOP (레포에서 PAID만 집계)
        var hotelRows = paymentRepository.hotelRevenueByYear(targetYear);
        var topHotels = new ArrayList<DashboardDetailDto.HotelRevenuePoint>();
        for (int i = 0; i < hotelRows.size() && i < topLimit; i++) {
            Object[] r = hotelRows.get(i);
            Long hotelId = r[0] != null ? ((Number) r[0]).longValue() : null;
            String hotelName = (String) r[1];
            Number rev = (Number) r[2];
            topHotels.add(DashboardDetailDto.HotelRevenuePoint.builder()
                    .hotelId(hotelId)
                    .hotelName(hotelName)
                    .revenue(rev != null ? rev.longValue() : 0L)
                    .build());
        }

        var dto = DashboardDetailDto.builder()
                .dailyRevenue(dailyRevenue)
                .dailySignups(dailySignup)
                .monthlySignups(monthly)
                .topHotels(topHotels)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}
