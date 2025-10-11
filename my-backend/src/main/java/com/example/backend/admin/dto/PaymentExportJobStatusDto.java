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
public class PaymentExportJobStatusDto {
    private String jobId;
    private String format;
    private Status status;
    private String message;
    private long totalRows;
    private long processedRows;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
    private String downloadPath;

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
