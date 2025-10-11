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
    String userEmail;
    Integer totalPrice;
    Integer amount;
    Integer basePrice;
    Integer tax;
    Integer discount;
    String paymentMethod;
    String paymentStatus;
    LocalDateTime createdAt;
    LocalDateTime canceledAt;
    String receiptUrl;
    Integer refundedAmount;
    Integer refundableAmount;
    Integer remainingAmount;
    Boolean partialRefundApplied;
    String memo;
}
