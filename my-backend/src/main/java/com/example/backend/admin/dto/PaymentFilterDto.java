package com.example.backend.admin.dto;

import com.example.backend.payment.domain.Payment;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFilterDto {
    private Payment.Status status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime from;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime to;

    private String hotelName;
    private String userName;

    @Min(0)
    private Integer minAmount;

    @Min(0)
    private Integer maxAmount;

    private String transactionId;
    private List<String> paymentMethods;

    @Positive
    private Long hotelId;

    private String quickFilter;
}
