// src/main/java/com/example/backend/payment/domain/Payment.java
package com.example.backend.payment.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import com.example.backend.authlogin.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "payment") // DB가 대문자 'Payment'면 여기만 "Payment"로 바꾸세요
@Getter
@Setter
@ToString(exclude = "user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    /** DB와 완전히 동일한 상태 집합 */
    public enum Status {
        PENDING, PAID, CANCELLED, REFUNDED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 예약 FK =====
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    // ===== 유저 연관관계 =====
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 읽기 편의를 위한 FK 미러(쓰기 금지) */
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    // ===== 결제 수단/금액 =====
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // 예: "TOSS:CARD", "TOSS:KAKAOPAY", "TOSS"

    @Column(name = "base_price", nullable = false)
    private Integer basePrice;

    @Column(name = "tax", nullable = false)
    private Integer tax;

    @Column(name = "discount", nullable = false)
    private Integer discount;

    /** 총 결제금액 */
    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    /** 위젯/검증용 금액(없으면 totalPrice로 보정) */
    @Column(name = "amount")
    private Integer amount;

    // ===== 주문/키/영수증 =====
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "order_name")
    private String orderName;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "receipt_url", length = 512)
    private String receiptUrl;

    // ===== 상태/타임스탬프 =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    // ===== 표시용 파생 속성 =====
    @Transient
    @JsonProperty("displayMethod")
    public String getDisplayMethod() {
        if (paymentMethod == null) return null;
        if (paymentMethod.startsWith("TOSS:")) {
            String sub = paymentMethod.substring("TOSS:".length());
            return switch (sub) {
                case "KAKAOPAY" -> "toss결제(카카오페이)";
                case "NAVERPAY" -> "toss결제(네이버페이)";
                case "TOSSPAY"  -> "toss결제(토스페이)";
                case "CARD"     -> "toss결제(카드)";
                default         -> "toss결제(" + sub.toLowerCase() + ")";
            };
        }
        if ("TOSS".equals(paymentMethod)) return "toss결제";
        return paymentMethod;
    }

    // ===== 기본값/합계 보정 =====
    @PrePersist
    @PreUpdate
    private void beforeSave() {
        if (paymentMethod == null || paymentMethod.isBlank()) paymentMethod = "TOSS";
        if (basePrice == null) basePrice = 0;
        if (tax == null) tax = 0;
        if (discount == null) discount = 0;

        if (totalPrice == null) {
            totalPrice = (amount != null) ? amount : (basePrice + tax - discount);
        }
        if (amount == null) amount = totalPrice;

        if (status == null) status = Status.PENDING;
    }
}
