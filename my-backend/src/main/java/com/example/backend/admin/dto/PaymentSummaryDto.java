package com.example.backend.admin.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PaymentSummaryDto {
    Long paymentId;
    Long reservationId;
    String transactionId;
    String hotelName;
    String userName;
    Integer totalPrice;
    String paymentMethod;
    String paymentStatus;
    LocalDateTime createdAt;
    LocalDateTime canceledAt;
}
