package com.example.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.transaction.Transactional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Payment findByOrderId(String orderId);

    @Transactional
    @Modifying
    @Query("UPDATE Payment p SET p.paymentKey = :paymentKey, p.amount = :amount WHERE p.orderId = :orderId")
    int updatePaymentByOrderId(String paymentKey, int amount, String orderId);
}
