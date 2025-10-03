package com.example.backend.admin.controller;

import com.example.backend.payment.domain.Payment;
import com.example.backend.admin.service.AdminPaymentService;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.dto.PaymentSummaryDto;
import com.example.backend.admin.dto.PaymentAnalyticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminPaymentController {
    private final AdminPaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentSummaryDto>>> list(@RequestParam(required = false) Payment.Status status,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                                  @RequestParam(required = false) String hotelName,
                                                                  @RequestParam(required = false) String userName,
                                                                  Pageable pageable) {
        log.info("결제 목록 API 호출 - status: {}, from: {}, to: {}, hotelName: {}, userName: {}",
                status, from, to, hotelName, userName);

        var page = paymentService.listWithDetails(status, from, to, hotelName, userName, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.get(id)));
    }

    /**
     * 결제 환불 처리
     * @param id 결제 ID
     * @return 환불 처리 결과
     */
    @PutMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<Void>> refund(@PathVariable Long id) {
        try {
            paymentService.refund(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            log.error("환불 처리 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(400).body(ApiResponse.fail(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("환불 처리 실패 - 상태 충돌: {}", e.getMessage());
            return ResponseEntity.status(409).body(ApiResponse.fail(e.getMessage()));
        }
    }



    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<PaymentAnalyticsDto>> analytics(@RequestParam String granularity,
                                                                       @RequestParam(required = false) Long hotelId,
                                                                       @RequestParam(required = false) String paymentMethod,
                                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
                                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to) {
        log.info("매출 통계 조회 - granularity: {}, hotelId: {}, paymentMethod: {}, from: {}, to: {}", 
                 granularity, hotelId, paymentMethod, from, to);
        try {
            PaymentAnalyticsDto analytics = paymentService.getAnalytics(granularity, hotelId, paymentMethod, from, to);
            log.info("매출 통계 조회 성공 - byPeriod: {}, byHotel: {}, byMethod: {}", 
                     analytics.getByPeriod().size(), analytics.getByHotel().size(), analytics.getByMethod().size());
            return ResponseEntity.ok(ApiResponse.ok(analytics));
        } catch (Exception e) {
            log.error("매출 통계 조회 실패", e);
            throw e;
        }
    }
}