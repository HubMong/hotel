package com.example.backend.admin.dto;

import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardMetricsDto {
    private long totalTodayAmount;      // 오늘 총 결제액
    private long totalTodayCount;       // 오늘 결제 건수
    private double refundRateToday;     // 오늘 환불율
    private double avgPaymentAmount;    // 평균 결제 금액 (기간)
    private long pendingCount;          // PENDING
    private long completedCount;        // COMPLETED
    private long cancelledCount;        // CANCELLED
    private long failedCount;           // FAILED
    private double dayOverDayAmountRate; // 전일 대비 증감률 (금액)
}
