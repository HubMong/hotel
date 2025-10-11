package com.example.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartialRefundResponse {
    private Long paymentId;
    private Long refundPaymentId;
    private Integer requestedAmount;
    private Integer fee;
    private Integer netAmount;
    private Integer refundedTotal;
    private Integer remainingAmount;
    private LocalDateTime processedAt;
    private String memo;
}
