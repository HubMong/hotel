// src/main/java/com/example/backend/admin/controller/AdminStatsController.java
package com.example.backend.admin.controller;

import com.example.backend.admin.repository.AdminPaymentRepository;
import com.example.backend.admin.repository.AdminReservationRepository;
import com.example.backend.admin.repository.UserRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminStatsController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final AdminReservationRepository reservationRepository;
    private final AdminPaymentRepository paymentRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String,Object>> dashboard() {
        Map<String,Object> m = new HashMap<>();
        m.put("totalUsers", userRepository.count());
        m.put("totalHotels", hotelRepository.count());
        m.put("totalReservations", reservationRepository.count());

        // 총매출: 결제 상태 PAID만(헬퍼 사용), 에폭~현재까지 합계
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime now   = LocalDateTime.now();
        Long totalRevenue   = paymentRepository.sumPaidTotalPriceByCreatedAtBetween(epoch, now);
        m.put("totalRevenue", totalRevenue != null ? totalRevenue : 0L);

        return ResponseEntity.ok(m);
    }

    @GetMapping("/sales")
    public ResponseEntity<Map<String,Object>> sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Map<String,Object> m = new HashMap<>();
        LocalDateTime fromLdt = from.atStartOfDay();
        LocalDateTime toLdt   = to.atTime(LocalTime.MAX);

        // 레포에서 상태 PAID로 고정 필터링(헬퍼 사용)
        Long totalRevenue = paymentRepository.sumPaidTotalPriceByCreatedAtBetween(fromLdt, toLdt);
        m.put("totalRevenue", totalRevenue != null ? totalRevenue : 0L);

        var rows = paymentRepository.hotelRevenueBetween(fromLdt, toLdt);
        List<Map<String,Object>> settlements = new ArrayList<>();
        long platformFee = 0L;

        for (Object[] r : rows) {
            Long hotelId = ((Number) r[0]).longValue();
            String hotelName = (String) r[1];
            Long revenue = ((Number) r[2]).longValue();
            Integer reservationCount = ((Number) r[3]).intValue();

            long fee = Math.round(revenue * 0.10);
            platformFee += fee;
            long settlementAmount = revenue - fee;

            Map<String,Object> item = new HashMap<>();
            item.put("hotelId", hotelId);
            item.put("hotelName", hotelName);
            item.put("totalRevenue", revenue);
            item.put("reservationCount", reservationCount);
            item.put("settlementAmount", settlementAmount);
            settlements.add(item);
        }
        m.put("platformFeeAmount", platformFee);
        m.put("hotelSettlements", settlements);
        return ResponseEntity.ok(m);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String,Object>> userStats(@RequestParam int year) {
        Map<String,Object> m = new HashMap<>();
        List<Object[]> rows = userRepository.countMonthlyUsers(year);
        m.put("year", year);
        m.put("monthly", rows);
        return ResponseEntity.ok(m);
    }

    @GetMapping("/reservations")
    public ResponseEntity<Map<String,Object>> reservationStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        ZoneId zone = ZoneId.systemDefault();
        var start = from.atStartOfDay(zone).toInstant();
        var end   = to.atTime(LocalTime.MAX).atZone(zone).toInstant();

        long count = reservationRepository.countByCreatedAtBetween(start, end);

        Map<String,Object> m = new HashMap<>();
        m.put("from", from);
        m.put("to", to);
        m.put("count", count);
        return ResponseEntity.ok(m);
    }

    @GetMapping("/hotels/ranking")
    public ResponseEntity<Map<String,Object>> hotelRanking(@RequestParam(required = false) Integer year) {
        Map<String,Object> m = new HashMap<>();
        var rows = paymentRepository.hotelRevenueByYear(year); // 레포에서 'PAID'만 집계
        m.put("year", year);
        m.put("ranking", rows);
        return ResponseEntity.ok(m);
    }

    @GetMapping("/cancellation-reasons")
    public ResponseEntity<Map<String,Object>> cancellationReasons() {
        Map<String,Object> m = new HashMap<>();
        m.put("reasons", List.of());
        return ResponseEntity.ok(m);
    }
}
