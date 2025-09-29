package com.example.backend.admin.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class AdminPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "base_price", nullable = false)
    private Integer basePrice;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "tax", nullable = false)
    private Integer tax;

    @Column(name = "discount", nullable = false)
    private Integer discount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "receipt_url")
    private String receiptUrl;

    public enum Status { PAID, CANCELLED, REFUNDED }
}
