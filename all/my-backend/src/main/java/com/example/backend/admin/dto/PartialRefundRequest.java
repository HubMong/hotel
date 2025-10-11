package com.example.backend.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartialRefundRequest {
    @NotNull
    @Min(1)
    private Integer amount; // 원 단위 환불 총액

    @Min(0)
    private Integer fee; // 공제 수수료 (최대 amount)

    @NotBlank
    private String reasonCategory;

    private String memo;

    private boolean sendEmail;

    private boolean markAsResolved;
}
