package com.example.backend.admin.controller;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.admin.service.AdminReservationService;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.dto.ReservationDetailDto;
import com.example.backend.admin.dto.ReservationCalendarDayDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminReservationController {

    private final AdminReservationService reservationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReservationDetailDto>>> list(
        @RequestParam(required = false) Reservation.Status status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(required = false, name = "stayFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime stayFrom,
        @RequestParam(required = false, name = "stayTo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime stayTo,
        @RequestParam(required = false) String hotelName,
        @RequestParam(required = false) String userName, 
        @RequestParam(required = false) String paymentStatus,
        Pageable pageable) {
        
        var page = reservationService.listWithDetails(status, from, to, stayFrom, stayTo, 
                                                     hotelName, userName, paymentStatus, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationDetailDto>> detail(@PathVariable Long id) {
        try {
            log.info("예약 상세 조회 요청 - ID: {}", id);
            ReservationDetailDto detail = reservationService.getDetail(id);
            log.info("예약 상세 조회 완료 - ID: {}", id);
            return ResponseEntity.ok(ApiResponse.ok(detail));
        } catch (RuntimeException e) {
            log.error("예약 상세 조회 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("예약 상세 조회 중 예상치 못한 오류 - ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("서버 오류가 발생했습니다."));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        try {
            reservationService.cancel(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            log.error("예약 취소 실패 - ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("예약 취소에 실패했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Void>> complete(@PathVariable Long id) {
        try {
            reservationService.complete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            log.error("예약 완료 처리 실패 - ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("예약 완료 처리에 실패했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Reservation.Status newStatus = Reservation.Status.valueOf(status.toUpperCase());
            reservationService.updateStatus(id, newStatus);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(ApiResponse.fail("잘못된 상태 값입니다: " + status));
        } catch (Exception e) {
            log.error("예약 상태 변경 실패 - ID: {}, Status: {}", id, status, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("예약 상태 변경에 실패했습니다: " + e.getMessage()));
        }
    }
    
    // 예약 달력 집계 조회
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<java.util.List<ReservationCalendarDayDto>>> calendar(
            @RequestParam("monthStart") String monthStart,
            @RequestParam("monthEnd") String monthEnd,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String userName) {
        LocalDateTime start = parseDateTimeFlexible(monthStart);
        LocalDateTime end = parseDateTimeFlexible(monthEnd);
        if (start != null) start = start.with(LocalTime.MIN);
        if (end != null) end = end.with(LocalTime.MAX);
        var days = reservationService.getCalendar(start, end, status, hotelId, userName);
        return ResponseEntity.ok(ApiResponse.ok(days));
    }

    private LocalDateTime parseDateTimeFlexible(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) { }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) { }
        try {
            return Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) { }
        throw new IllegalArgumentException("잘못된 날짜 형식입니다: " + value);
    }
    
}
