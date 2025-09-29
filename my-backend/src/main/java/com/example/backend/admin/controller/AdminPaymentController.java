package com.example.backend.admin.controller;

import com.example.backend.admin.service.AdminPaymentService;
import com.example.backend.payment.domain.Payment;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.dto.PaymentSummaryDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminPaymentController {

    private final AdminPaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentSummaryDto>>> list(
            @RequestParam(required = false) Payment.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String hotelName,
            @RequestParam(required = false) String userName,
            Pageable pageable
    ) {
        log.info("결제 목록 API 호출 - status: {}, from: {}, to: {}, hotelName: {}, userName: {}",
                status, from, to, hotelName, userName);

        // FE에서 paidAt 정렬 요청 → BE의 createdAt으로 매핑
        Sort sort = pageable.getSort().isUnsorted()
                ? Sort.unsorted()
                : pageable.getSort().stream()
                    .map(order -> order.getProperty().equals("paidAt")
                            ? order.withProperty("createdAt")
                            : order)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), Sort::by));

        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        var page = paymentService.listWithDetails(status, from, to, hotelName, userName, newPageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.get(id)));
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<Void>> refund(@PathVariable Long id) {
        paymentService.refund(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
