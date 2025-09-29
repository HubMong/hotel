// src/main/java/com/example/backend/admin/service/AdminPaymentService.java
package com.example.backend.admin.service;

import com.example.backend.admin.dto.PaymentSummaryDto;
import com.example.backend.admin.repository.AdminPaymentRepository;
import com.example.backend.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final AdminPaymentRepository paymentRepository;

    public Page<Payment> list(Payment.Status status,
                              LocalDateTime from, LocalDateTime to,
                              LocalDateTime refundFrom, LocalDateTime refundTo,
                              Pageable pageable) {
        return paymentRepository.search(status, from, to, refundFrom, refundTo, pageable);
    }

    public Page<PaymentSummaryDto> listWithDetails(Payment.Status status,
                                                   LocalDateTime from, LocalDateTime to,
                                                   String hotelName, String userName,
                                                   Pageable pageable) {
        String statusStr = status != null ? status.name() : null;

        log.info("결제 목록 조회 - status: {}, from: {}, to: {}, hotelName: {}, userName: {}",
                statusStr, from, to, hotelName, userName);

        Page<Object[]> results = paymentRepository.searchWithDetails(statusStr, from, to, hotelName, userName, pageable);

        log.info("조회된 결제 결과 수: {}", results.getContent().size());

        List<PaymentSummaryDto> dtos = results.getContent().stream()
                .map(this::mapToPaymentSummaryDto)
                .toList();

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    public Payment get(Long id) {
        return paymentRepository.findById(id).orElseThrow();
    }

    // 상태 비교/설정 모두 Payment.Status로 통일
    public void refund(Long id) {
        Payment p = paymentRepository.findById(id).orElseThrow();
        if (p.getStatus() == Payment.Status.PAID) {
            p.setStatus(Payment.Status.REFUNDED);
            p.setRefundedAt(LocalDateTime.now());
            paymentRepository.save(p);
        }
    }

    private PaymentSummaryDto mapToPaymentSummaryDto(Object[] row) {
        if (row == null || row.length < 10) {
            log.warn("결제 데이터 매핑 오류: 최소 10개 필드가 필요하지만 {}개만 조회됨", row != null ? row.length : 0);
            throw new IllegalArgumentException("조회된 결제 데이터가 불완전합니다");
        }

        return PaymentSummaryDto.builder()
                .paymentId(safeLong(row[0]))
                .reservationId(safeLong(row[1]))
                .transactionId(safeString(row[2]))
                .hotelName(safeString(row[3]))
                .userName(safeString(row[4]))
                .totalPrice(safeInteger(row[5]))
                .paymentMethod(safeString(row[6]))
                .paymentStatus(safeString(row[7]))
                .paymentCreatedAt(safeDateTime(row[8]))
                .refundedAt(safeDateTime(row[9]))
                .build();
    }

    private Long safeLong(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).longValue();
            if (obj instanceof String) return Long.parseLong((String) obj);
        } catch (Exception e) {
            log.warn("Long 변환 실패: {}", obj, e);
        }
        return null;
    }

    private Integer safeInteger(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
        } catch (Exception e) {
            log.warn("Integer 변환 실패: {}", obj, e);
        }
        return null;
    }

    private String safeString(Object obj) {
        if (obj == null) return null;
        try {
            return obj.toString().trim();
        } catch (Exception e) {
            log.warn("String 변환 실패: {}", obj, e);
            return null;
        }
    }

    private LocalDateTime safeDateTime(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
            if (obj instanceof LocalDateTime ldt) return ldt;
            if (obj instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
            if (obj instanceof java.util.Date d2) return d2.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            log.warn("LocalDateTime 변환 실패: {}", obj, e);
        }
        return null;
    }
}
