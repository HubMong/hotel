package com.example.backend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.transaction.Transactional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Payment findByOrderId(String orderId);
}
