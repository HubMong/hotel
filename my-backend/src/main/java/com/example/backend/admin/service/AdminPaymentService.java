package com.example.backend.admin.service;

import com.example.backend.payment.domain.Payment;
import com.example.backend.payment.repository.PaymentRepository;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.repository.ReservationRepository;

import jakarta.transaction.Transactional;

import com.example.backend.admin.dto.PaymentSummaryDto;
import com.example.backend.admin.dto.PaymentAnalyticsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    public Page<Payment> list(Payment.Status status, LocalDateTime from, LocalDateTime to,
                              Pageable pageable) {
        return paymentRepository.search(status, from, to, pageable);
    }

    // 상세 정보 포함 결제 목록 조회
    public Page<PaymentSummaryDto> listWithDetails(Payment.Status status, LocalDateTime from, LocalDateTime to,
                                                   String hotelName, String userName,
                                                   Pageable pageable) {
        String statusStr = status != null ? status.name() : null;

        log.info("결제 목록 조회 - status: {}, from: {}, to: {}, hotelName: {}, userName: {}",
                statusStr, from, to, hotelName, userName);

        // 정렬이 없으면 기본값으로 created_at DESC 사용
        if (pageable.getSort().isUnsorted()) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("createdAt")
                )
            );
        }

        // Pageable의 정렬 필드를 DB 컬럼명으로 변환
        Pageable mappedPageable = mapSortFieldsToDbColumns(pageable);

        Page<Object[]> results = paymentRepository.searchWithDetails(statusStr, from, to, hotelName, userName, mappedPageable);

        log.info("조회된 결제 결과 수: {}", results.getContent().size());

        List<PaymentSummaryDto> dtos = results.getContent().stream()
            .map(this::mapToPaymentSummaryDto)
            .toList();

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    public Payment get(Long id) { return paymentRepository.findById(id).orElseThrow(); }

    /**
     * 결제 환불 처리
     * - Payment 상태를 CANCELLED로 변경
     * - 연결된 Reservation도 CANCELLED로 변경 (데이터 일관성 유지)
     * @param id 결제 ID
     */
    @Transactional
    public void refund(Long id) {
        log.info("환불 처리 시작 - paymentId: {}", id);
        
        // 1. 결제 정보 조회 (User 엔티티 함께 로드하여 DataIntegrityViolationException 방지)
        Payment p = paymentRepository.findByIdWithUser(id)
            .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + id));
        
        log.info("결제 정보 로드 완료 - paymentId: {}, reservationId: {}, userId: {}, status: {}", 
            p.getId(), p.getReservationId(), p.getUserId(), p.getStatus());
        
        // 2. 결제 상태 검증
        if (p.getStatus() != Payment.Status.COMPLETED) {
            throw new IllegalStateException("완료된 결제만 환불할 수 있습니다. 현재 상태: " + p.getStatus());
        }
        
        // 3. 연결된 Reservation 조회 및 상태 변경
        Long reservationId = p.getReservationId();
        if (reservationId != null) {
            Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalStateException("연결된 예약 정보를 찾을 수 없습니다."));

            log.info("예약 정보 로드 - reservationId: {}, status: {}", 
                reservation.getId(), reservation.getStatus());

            if (reservation.getStatus() == Reservation.Status.CANCELLED) {
                log.info("예약이 이미 취소 상태 - reservationId: {}", reservation.getId());
            } else {
                reservation.setStatus(Reservation.Status.CANCELLED);
            }

            // 예약 상태를 먼저 DB에 반영하여 제약조건 위반 방지
            reservationRepository.saveAndFlush(reservation);

            log.info("예약 취소 완료 - reservationId: {}, newStatus: {}", 
                reservation.getId(), reservation.getStatus());
        } else {
            throw new IllegalStateException("결제에 연결된 예약 ID가 존재하지 않습니다.");
        }
        
        // 4. 결제 상태 변경
        p.setStatus(Payment.Status.CANCELLED);
        p.setCanceledAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.saveAndFlush(p);
        log.info("환불 처리 완료 - paymentId: {}, canceledAt: {}, status: {}", 
            saved.getId(), saved.getCanceledAt(), saved.getStatus());
    }



    public PaymentAnalyticsDto getAnalytics(String granularity, Long hotelId, String paymentMethod,
                                            java.time.LocalDate from, java.time.LocalDate to) {
        java.time.LocalDateTime start = from.atStartOfDay();
        java.time.LocalDateTime end = to.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Object[]> periodRows;
        if ("week".equalsIgnoreCase(granularity)) {
            periodRows = paymentRepository.aggregateWeekly(start, end, hotelId, paymentMethod);
        } else if ("month".equalsIgnoreCase(granularity)) {
            periodRows = paymentRepository.aggregateMonthly(start, end, hotelId, paymentMethod);
        } else {
            periodRows = paymentRepository.aggregateDaily(start, end, hotelId, paymentMethod);
        }

        List<Object[]> hotelRows = paymentRepository.aggregateByHotel(start, end, hotelId, paymentMethod);
        List<Object[]> methodRows = paymentRepository.aggregateByMethod(start, end, hotelId, paymentMethod);

        List<PaymentAnalyticsDto.TimeBucket> byPeriod = periodRows.stream()
                .map(row -> PaymentAnalyticsDto.TimeBucket.builder()
                        .period(row[0].toString())
                        .amount(((Number) row[1]).longValue())
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        List<PaymentAnalyticsDto.CategoryBucket> byHotel = hotelRows.stream()
                .map(row -> PaymentAnalyticsDto.CategoryBucket.builder()
                        .label((String) row[0])
                        .amount(((Number) row[1]).longValue())
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        List<PaymentAnalyticsDto.CategoryBucket> byMethod = methodRows.stream()
                .map(row -> PaymentAnalyticsDto.CategoryBucket.builder()
                        .label((String) row[0])
                        .amount(((Number) row[1]).longValue())
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        return PaymentAnalyticsDto.builder()
                .byPeriod(byPeriod)
                .byHotel(byHotel)
                .byMethod(byMethod)
                .build();
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
            .createdAt(safeDateTime(row[8]))
            .canceledAt(safeDateTime(row[9]))
            .build();
    }

    private Long safeLong(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).longValue();
            if (obj instanceof String) return Long.parseLong((String) obj);
            return null;
        } catch (Exception e) {
            log.warn("Long 변환 실패: {}", obj, e);
            return null;
        }
    }

    private Integer safeInteger(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
            return null;
        } catch (Exception e) {
            log.warn("Integer 변환 실패: {}", obj, e);
            return null;
        }
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
            if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime();
            if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
            if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate().atStartOfDay();
            if (obj instanceof java.util.Date) return ((java.util.Date) obj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            return null;
        } catch (Exception e) {
            log.warn("LocalDateTime 변환 실패: {}", obj, e);
            return null;
        }
    }

    /**
     * Pageable의 정렬 필드명을 DB 컬럼명으로 매핑
     * Native Query에서는 DB 컬럼명(snake_case)을 사용해야 함
     */
    private Pageable mapSortFieldsToDbColumns(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        java.util.Map<String, String> fieldMapping = java.util.Map.of(
            "id", "paymentId",
            "createdAt", "createdAt",
            "amount", "totalPrice",
            "status", "paymentStatus"
        );

        java.util.List<org.springframework.data.domain.Sort.Order> mappedOrders = new java.util.ArrayList<>();
        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
            String dbColumn = fieldMapping.getOrDefault(order.getProperty(), order.getProperty());
            mappedOrders.add(new org.springframework.data.domain.Sort.Order(order.getDirection(), dbColumn));
        }

        return org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            org.springframework.data.domain.Sort.by(mappedOrders)
        );
    }
}