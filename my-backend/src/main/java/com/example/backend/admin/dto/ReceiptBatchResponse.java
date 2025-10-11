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
public class ReceiptBatchResponse {
    private String jobId;
    private int totalReceipts;
    private LocalDateTime requestedAt;
    private PaymentExportJobStatusDto.Status status;
}
