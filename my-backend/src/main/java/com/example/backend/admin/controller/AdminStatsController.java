package com.example.backend.admin.controller;

import com.example.backend.payment.repository.PaymentRepository;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.admin.repository.AdminUserRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminStatsController {

    private final AdminUserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String,Object>> dashboard() {
        Map<String,Object> m = new HashMap<>();
        m.put("totalUsers", userRepository.count());
        m.put("totalHotels", hotelRepository.count());
        m.put("totalReservations", reservationRepository.count());
        m.put("totalRevenue", paymentRepository.findAll().stream().mapToLong(p -> p.getTotalPrice() == null ? 0 : p.getTotalPrice()).sum());
        return ResponseEntity.ok(m);
    }

    @GetMapping("/sales")
    public ResponseEntity<Map<String,Object>> sales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        Map<String,Object> m = new HashMap<>();
        var start = from.atStartOfDay();
        var end = to.atTime(java.time.LocalTime.MAX);

        // 총 매출
    Long totalRevenue = paymentRepository.sumTotalPriceByCreatedAtBetween(start, end, com.example.backend.payment.domain.Payment.Status.COMPLETED);
        m.put("totalRevenue", totalRevenue != null ? totalRevenue : 0L);

        // 호텔별 매출/플랫폼 수수료/예약건수
        var rows = paymentRepository.hotelRevenueBetween(start, end);
        java.util.List<Map<String,Object>> hotelSettlements = new java.util.ArrayList<>();
        long platformFee = 0L;
        for (Object[] r : rows) {
            Long hotelId = ((Number) r[0]).longValue();
            String hotelName = (String) r[1];
            Long revenue = ((Number) r[2]).longValue();
            Integer reservationCount = ((Number) r[3]).intValue();
            long fee = Math.round(revenue * 0.10);
            platformFee += fee;
            long settlementAmount = revenue - fee; // 90%
            Map<String,Object> item = new HashMap<>();
            item.put("hotelId", hotelId);
            item.put("hotelName", hotelName);
            item.put("totalRevenue", revenue);
            item.put("reservationCount", reservationCount);
            item.put("settlementAmount", settlementAmount);
            item.put("platformFee", fee);
            hotelSettlements.add(item);
        }
        m.put("platformFeeAmount", platformFee);
        m.put("hotelSettlements", hotelSettlements);
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
    public ResponseEntity<Map<String,Object>> reservationStats(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Map<String,Object> m = new HashMap<>();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.MAX);
        java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
        java.time.Instant startInstant = start.atZone(zoneId).toInstant();
        java.time.Instant endInstant = end.atZone(zoneId).toInstant();
        long count = reservationRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(startInstant) && !r.getCreatedAt().isAfter(endInstant))
                .count();
        m.put("from", from);
        m.put("to", to);
        m.put("count", count);
        return ResponseEntity.ok(m);
    }

    @GetMapping("/hotels/ranking")
    public ResponseEntity<Map<String,Object>> hotelRanking(@RequestParam(required = false) Integer year) {
        Map<String,Object> m = new HashMap<>();
        var rows = paymentRepository.hotelRevenueByYear(year);
        m.put("year", year);
        m.put("ranking", rows);
        return ResponseEntity.ok(m);
    }

    @GetMapping("/cancellation-reasons")
    public ResponseEntity<Map<String,Object>> cancellationReasons() {
        Map<String,Object> m = new HashMap<>();
        // Placeholder: no cancellation reason column in schema; return empty
        m.put("reasons", List.of());
        return ResponseEntity.ok(m);
    }
}
