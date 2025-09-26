package com.example.backend.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String orderName;
    private int amount;

    private Long reservationId;
    private Long userId;
    private String customerName;
    private String email;
    private String phone;

    private String paymentKey;

    private String status;
}
