package com.example.backend.admin.controller;

import com.example.backend.payment.domain.Payment;
import com.example.backend.admin.service.AdminPaymentService;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.dto.PaymentSummaryDto;
import com.example.backend.admin.dto.PaymentAnalyticsDto;
import com.example.backend.admin.dto.PaymentExportRequest;
import com.example.backend.admin.dto.PaymentExportJobStatusDto;
import com.example.backend.admin.dto.SettlementReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.time.LocalDate;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {
    private final AdminPaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentSummaryDto>>> list(@RequestParam(required = false) Payment.Status status,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                                  @RequestParam(required = false) String hotelName,
                                                                  @RequestParam(required = false) String userName,
                                                                 @RequestParam(required = false) Long hotelId,
                                                                  @RequestParam(required = false) Integer minAmount,
                                                                  @RequestParam(required = false) Integer maxAmount,
                                                                  @RequestParam(required = false) String transactionId,
                                                                  @RequestParam(required = false) List<String> paymentMethods,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size,
                                                                  @RequestParam(required = false) String sort,
                                                                  @RequestParam(required = false) String direction) {
        log.info("결제 목록 API 호출 - status: {}, from: {}, to: {}, hotelName: {}, userName: {}",
                status, from, to, hotelName, userName);
        Sort s = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            Sort.Direction dir = (direction != null && direction.equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
            s = Sort.by(dir, sort);
        }
    Pageable pageable = PageRequest.of(page, size, s);
    Page<PaymentSummaryDto> resultPage = paymentService.listWithDetails(status, from, to, hotelName, userName, hotelId,
        minAmount, maxAmount, transactionId, paymentMethods, null, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(resultPage)));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<com.example.backend.admin.dto.DashboardMetricsDto>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.dashboard()));
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportCsv(@RequestParam(required = false) Payment.Status status,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                       @RequestParam(required = false) String hotelName,
                                       @RequestParam(required = false) String userName,
                                       @RequestParam(required = false) Long hotelId,
                                       @RequestParam(required = false) Integer minAmount,
                                       @RequestParam(required = false) Integer maxAmount,
                                       @RequestParam(required = false) String transactionId,
                                       @RequestParam(required = false) List<String> paymentMethods) {
        Pageable pageable = PageRequest.of(0, 5000); // export limit
    Page<PaymentSummaryDto> resultPage = paymentService.listWithDetails(status, from, to, hotelName, userName, hotelId,
        minAmount, maxAmount, transactionId, paymentMethods, null, pageable);
        String csv = paymentService.exportCsv(resultPage.getContent());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=payments.csv")
                .body(csv);
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

    @PostMapping("/export/jobs")
    public ResponseEntity<ApiResponse<PaymentExportJobStatusDto>> createExportJob(@Valid @RequestBody PaymentExportRequest request) {
        PaymentExportJobStatusDto status = paymentService.requestExport(request);
        HttpStatus code = status.getStatus() == PaymentExportJobStatusDto.Status.COMPLETED ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
        return ResponseEntity.status(code).body(ApiResponse.ok(status));
    }

    @GetMapping("/export/jobs/{jobId}")
    public ResponseEntity<ApiResponse<PaymentExportJobStatusDto>> getExportJobStatus(@PathVariable String jobId) {
        return paymentService.getExportJobStatus(jobId)
                .map(status -> ResponseEntity.ok(ApiResponse.ok(status)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("요청한 내보내기 작업을 찾을 수 없습니다.")));
    }

    @GetMapping("/export/jobs/{jobId}/download")
    public ResponseEntity<ByteArrayResource> downloadExport(@PathVariable String jobId) {
        return paymentService.getExportPayload(jobId)
                .map(payload -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(payload.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + payload.getFilename())
                        .body(payload.getResource()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/settlements")
    public ResponseEntity<ApiResponse<SettlementReportDto>> settlementReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long hotelId) {
        SettlementReportDto report = paymentService.settlementReport(from, to, hotelId);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}