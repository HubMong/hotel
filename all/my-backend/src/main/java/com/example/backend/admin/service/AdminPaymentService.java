package com.example.backend.admin.service;

import com.example.backend.payment.domain.Payment;
import com.example.backend.admin.repository.AdminPaymentRepository;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.repository.ReservationRepository;

import org.springframework.transaction.annotation.Transactional;

import com.example.backend.admin.dto.PaymentSummaryDto;
import com.example.backend.admin.dto.PaymentAnalyticsDto;
import com.example.backend.admin.dto.DashboardMetricsDto;
import com.example.backend.admin.dto.PaymentFilterDto;
import com.example.backend.admin.dto.PaymentExportRequest;
import com.example.backend.admin.dto.PaymentExportJobStatusDto;
import com.example.backend.admin.dto.SettlementReportDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {
    private final AdminPaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final AdminPaymentExportService exportService;

    private static final int EXPORT_PAGE_SIZE = 5000;
    private static final int EXPORT_MAX_PAGES = 50;
    private static final double PLATFORM_FEE_RATE = 0.08d;

    public Page<Payment> list(Payment.Status status, LocalDateTime from, LocalDateTime to,
                              Pageable pageable) {
        return paymentRepository.search(status, from, to, pageable);
    }

    // 상세 정보 포함 결제 목록 조회
    public Page<PaymentSummaryDto> listWithDetails(Payment.Status status, LocalDateTime from, LocalDateTime to,
                                                   String hotelName, String userName, Long hotelId,
                                                   Integer minAmount, Integer maxAmount,
                                                   String transactionId,
                                                   List<String> paymentMethods,
                                                   List<Long> paymentIds,
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

        Page<Object[]> results = paymentRepository.searchWithDetails(statusStr, from, to, hotelName, userName,
            hotelId,
            minAmount, maxAmount, transactionId,
            (paymentMethods == null || paymentMethods.isEmpty()) ? null : paymentMethods,
            (paymentIds == null || paymentIds.isEmpty()) ? null : paymentIds,
            mappedPageable);

        log.info("조회된 결제 결과 수: {}", results.getContent().size());

        List<PaymentSummaryDto> dtos = results.getContent().stream()
            .map(this::mapToPaymentSummaryDto)
            .toList();

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    public Payment get(Long id) { return paymentRepository.findById(id).orElseThrow(); }

    // 대시보드 지표
    public DashboardMetricsDto dashboard() {
        LocalDateTime startToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endToday = startToday.plusDays(1).minusNanos(1);
        LocalDateTime startYesterday = startToday.minusDays(1);
        LocalDateTime endYesterday = startToday.minusNanos(1);

        Long todayCompletedAmount = paymentRepository.sumCompletedBetween(startToday, endToday);
        if (todayCompletedAmount == null) todayCompletedAmount = 0L;
        Long yesterdayCompletedAmount = paymentRepository.sumCompletedBetween(startYesterday, endYesterday);
        if (yesterdayCompletedAmount == null) yesterdayCompletedAmount = 0L;

        // 오늘 생성된 결제 건수를 상태별로 집계 (현재 상태 기준)
    EnumMap<Payment.Status, Long> statusCountMap = new EnumMap<>(Payment.Status.class);
        paymentRepository.countStatusesBetween(startToday, endToday).forEach(row -> {
            if (row == null || row.length < 2 || row[0] == null) {
                return;
            }
            Payment.Status status = (Payment.Status) row[0];
            long count = (row[1] instanceof Number) ? ((Number) row[1]).longValue() : 0L;
            statusCountMap.put(status, count);
        });

        long pending = statusCountMap.getOrDefault(Payment.Status.PENDING, 0L);
        long completed = statusCountMap.getOrDefault(Payment.Status.COMPLETED, 0L);
        long failed = statusCountMap.getOrDefault(Payment.Status.FAILED, 0L);
        long cancelled = statusCountMap.getOrDefault(Payment.Status.CANCELLED, 0L);

        long totalTodayCount = pending + completed + cancelled + failed;

        long totalAmountForAvgWindow = todayCompletedAmount;
        long totalCountForAvgWindow = Math.max(completed, 1);
        double avgPaymentAmount = (double) totalAmountForAvgWindow / totalCountForAvgWindow;

        double refundRateToday = (completed + cancelled) == 0
                ? 0d
                : (double) cancelled / (completed + cancelled) * 100d;

        double dayOverDayAmountRate = (yesterdayCompletedAmount == 0) ? (todayCompletedAmount > 0 ? 100d : 0d) :
                ((double) (todayCompletedAmount - yesterdayCompletedAmount) / yesterdayCompletedAmount) * 100d;

        return DashboardMetricsDto.builder()
                .totalTodayAmount(todayCompletedAmount)
                .totalTodayCount(totalTodayCount)
                .refundRateToday(Math.round(refundRateToday * 10d) / 10d)
                .avgPaymentAmount(Math.round(avgPaymentAmount))
                .pendingCount(pending)
                .completedCount(completed)
                .cancelledCount(cancelled)
                .failedCount(failed)
                .dayOverDayAmountRate(Math.round(dayOverDayAmountRate * 10d) / 10d)
                .build();
    }

    // CSV Export (간단 구현)
    public String exportCsv(List<PaymentSummaryDto> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("paymentId,reservationId,transactionId,hotelName,userName,totalPrice,paymentMethod,paymentStatus,createdAt,canceledAt\n");
        for (PaymentSummaryDto dto : rows) {
            sb.append(nullSafe(dto.getPaymentId())).append(',')
              .append(nullSafe(dto.getReservationId())).append(',')
              .append(escape(dto.getTransactionId())).append(',')
              .append(escape(dto.getHotelName())).append(',')
              .append(escape(dto.getUserName())).append(',')
              .append(nullSafe(dto.getTotalPrice())).append(',')
              .append(escape(dto.getPaymentMethod())).append(',')
              .append(escape(dto.getPaymentStatus())).append(',')
              .append(dto.getCreatedAt()).append(',')
              .append(dto.getCanceledAt()).append('\n');
        }
        return sb.toString();
    }

    private String escape(String v) { if (v == null) return ""; return '"' + v.replace("\"", "'") + '"'; }
    private String nullSafe(Object o) { return o == null ? "" : o.toString(); }

    /**
     * 결제 환불 처리
     * - Payment 상태를 CANCELLED로 변경
     * - 연결된 Reservation도 CANCELLED로 변경 (데이터 일관성 유지)
     * @param id 결제 ID
     */
    @Transactional
    @CacheEvict(cacheNames = "adminPaymentDashboard", allEntries = true)
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

        List<Object[]> periodRows = switch (granularity == null ? "day" : granularity.toLowerCase()) {
            case "week" -> paymentRepository.aggregateWeekly(start, end, hotelId, paymentMethod);
            case "month" -> paymentRepository.aggregateMonthly(start, end, hotelId, paymentMethod);
            default -> paymentRepository.aggregateDaily(start, end, hotelId, paymentMethod);
        };

        List<Object[]> hotelRows = paymentRepository.aggregateByHotel(start, end, hotelId, paymentMethod);
        List<Object[]> methodRows = paymentRepository.aggregateByMethod(start, end, hotelId, paymentMethod);

        List<PaymentAnalyticsDto.TimeBucket> byPeriod = periodRows.stream()
            .map(row -> PaymentAnalyticsDto.TimeBucket.builder()
                .period(row[0].toString())
                .amount(((Number) row[1]).longValue())
                .count(((Number) row[2]).longValue())
                .build())
            .toList();

        List<PaymentAnalyticsDto.CategoryBucket> byHotel = hotelRows.stream()
            .map(row -> PaymentAnalyticsDto.CategoryBucket.builder()
                .label((String) row[0])
                .amount(((Number) row[1]).longValue())
                .count(((Number) row[2]).longValue())
                .build())
            .toList();

        List<PaymentAnalyticsDto.CategoryBucket> byMethod = methodRows.stream()
            .map(row -> PaymentAnalyticsDto.CategoryBucket.builder()
                .label((String) row[0])
                .amount(((Number) row[1]).longValue())
                .count(((Number) row[2]).longValue())
                .build())
            .toList();

        return PaymentAnalyticsDto.builder()
            .byPeriod(byPeriod)
            .byHotel(byHotel)
            .byMethod(byMethod)
            .build();
    }

    public PaymentExportJobStatusDto requestExport(PaymentExportRequest request) {
        log.info("결제 내역 내보내기 요청 - format: {}, scope: {}", request.getFormat(), request.getScope());
        return exportService.startExportJob(request, () -> collectExportRows(request));
    }

    public Optional<PaymentExportJobStatusDto> getExportJobStatus(String jobId) {
        return exportService.getJobStatus(jobId);
    }

    public Optional<AdminPaymentExportService.ExportPayload> getExportPayload(String jobId) {
        return exportService.getPayload(jobId);
    }

    public void purgeExportJob(String jobId) {
        exportService.purge(jobId);
    }

    @Transactional(readOnly = true)
    public SettlementReportDto settlementReport(LocalDate from, LocalDate to, Long hotelId) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay().minusSeconds(1) : null;
        List<Object[]> rows = paymentRepository.settlementSummary(start, end, hotelId);
        List<SettlementReportDto.SettlementLine> lines = new ArrayList<>();
        long totalGross = 0;
        long totalVat = 0;
        long totalFee = 0;
        long totalNet = 0;

        for (Object[] row : rows) {
            long gross = ((Number) row[2]).longValue();
            long vat = ((Number) row[3]).longValue();
            long fee = Math.round(gross * PLATFORM_FEE_RATE);
            long net = gross - fee - vat;

            SettlementReportDto.SettlementLine line = SettlementReportDto.SettlementLine.builder()
                    .hotelId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .hotelName((String) row[1])
                    .grossAmount(gross)
                    .vat(vat)
                    .platformFee(fee)
                    .netPayout(net)
                    .reservationCount(((Number) row[4]).longValue())
                    .build();
            lines.add(line);

            totalGross += gross;
            totalVat += vat;
            totalFee += fee;
            totalNet += net;
        }

        return SettlementReportDto.builder()
                .from(from)
                .to(to)
                .lines(lines)
                .totalGross(totalGross)
                .totalVat(totalVat)
                .totalPlatformFee(totalFee)
                .totalNetPayout(totalNet)
                .build();
    }

    private List<PaymentSummaryDto> collectExportRows(PaymentExportRequest request) {
        PaymentFilterDto filters = Optional.ofNullable(request.getFilters()).orElseGet(PaymentFilterDto::new);
        List<Long> paymentIds = request.getScope() == PaymentExportRequest.Scope.SELECTION
                ? Optional.ofNullable(request.getSelectedIds()).orElse(List.of())
                : null;

        List<PaymentSummaryDto> results = new ArrayList<>();
        Pageable pageable = PageRequest.of(0, EXPORT_PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt")));
        Page<PaymentSummaryDto> page = fetchSummaryPage(filters, paymentIds, pageable);
        results.addAll(page.getContent());

        if (request.getScope() == PaymentExportRequest.Scope.ALL) {
            int loops = 1;
            while (page.hasNext() && loops < EXPORT_MAX_PAGES) {
                pageable = page.nextPageable();
                page = fetchSummaryPage(filters, paymentIds, pageable);
                results.addAll(page.getContent());
                loops++;
            }
        }
        return results;
    }

    private Page<PaymentSummaryDto> fetchSummaryPage(PaymentFilterDto filters,
                                                     List<Long> paymentIds,
                                                     Pageable pageable) {
        return listWithDetails(filters != null ? filters.getStatus() : null,
                filters != null ? filters.getFrom() : null,
                filters != null ? filters.getTo() : null,
                filters != null ? filters.getHotelName() : null,
                filters != null ? filters.getUserName() : null,
                filters != null ? filters.getHotelId() : null,
                filters != null ? filters.getMinAmount() : null,
                filters != null ? filters.getMaxAmount() : null,
                filters != null ? filters.getTransactionId() : null,
                filters != null ? filters.getPaymentMethods() : null,
                paymentIds,
                pageable);
    }

    private PaymentSummaryDto mapToPaymentSummaryDto(Object[] row) {
        if (row == null || row.length < 22) {
            log.warn("결제 데이터 매핑 오류: 최소 22개 필드가 필요하지만 {}개만 조회됨", row != null ? row.length : 0);
            throw new IllegalArgumentException("조회된 결제 데이터가 불완전합니다");
        }

        return PaymentSummaryDto.builder()
            .paymentId(safeLong(row[0]))
            .reservationId(safeLong(row[1]))
            .transactionId(safeString(row[2]))
            .hotelName(safeString(row[3]))
            .userName(safeString(row[4]))
            .userEmail(safeString(row[5]))
            .totalPrice(safeInteger(row[6]))
            .amount(safeInteger(row[7]))
            .basePrice(safeInteger(row[8]))
            .tax(safeInteger(row[9]))
            .discount(safeInteger(row[10]))
            .paymentMethod(safeString(row[11]))
            .paymentStatus(safeString(row[12]))
            .createdAt(safeDateTime(row[13]))
            .canceledAt(safeDateTime(row[14]))
            .receiptUrl(safeString(row[15]))
            .memo(safeString(row[16]))
            .refundedAmount(safePositive(row[18]))
            .remainingAmount(safeInteger(row[19]))
            .refundableAmount(safeInteger(row[20]))
            .partialRefundApplied(safeBoolean(row[21]))
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

    private Integer safePositive(Object obj) {
        Integer value = safeInteger(obj);
        if (value == null) {
            return null;
        }
        return Math.abs(value);
    }

    private Boolean safeBoolean(Object obj) {
        if (obj == null) return Boolean.FALSE;
        if (obj instanceof Boolean b) {
            return b;
        }
        if (obj instanceof Number n) {
            return n.intValue() != 0;
        }
        if (obj instanceof String s) {
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        }
        return Boolean.FALSE;
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