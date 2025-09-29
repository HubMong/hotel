package com.example.backend.admin.controller;

import com.example.backend.admin.dto.*;
import com.example.backend.admin.repository.AdminPaymentRepository;
import com.example.backend.admin.service.AdminReservationService;
import com.example.backend.hotel_reservation.domain.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/admin/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminReservationController {

    private final AdminReservationService reservationService;
    private final AdminPaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReservationDetailDto>>> list(
            @RequestParam(required = false) ReservationStatus status,
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
            var detail = reservationService.getDetail(id);
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
        reservationService.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 디버깅용: 결제 데이터 상태 확인
    @GetMapping("/debug/payment-status")
    public ResponseEntity<ApiResponse<Object>> debugPaymentStatus() {
        Long totalPayments = paymentRepository.countAllPayments();
        var allPayments = paymentRepository.findAll();

        Object debugInfo = java.util.Map.of(
                "totalPayments", totalPayments,
                "samplePayments", allPayments.stream().limit(5).toList()
        );

        return ResponseEntity.ok(ApiResponse.ok(debugInfo));
    }
}
