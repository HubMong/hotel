package com.example.backend.payment.config;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.Reservation.Status;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.payment.domain.Payment;
import com.example.backend.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    @Value("${payment.expired.batch-size:150}")
    private int batchSize;

    @Value("${payment.expired.max-iterations:3}")
    private int maxIterations;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Transactional
    @Scheduled(fixedDelayString = "${payment.expired.fixed-delay-ms:300000}")
    public void processExpiredPayments() {
        if (!running.compareAndSet(false, true)) {
            log.debug("Skip payment expiry run because previous execution is still running");
            return;
        }

        try {
            int iteration = 0;
            int processed = 0;
            LocalDateTime now = LocalDateTime.now();

            while (iteration++ < maxIterations) {
                List<Payment> expiredPayments = paymentRepository
                        .findByExpireAtBeforeAndStatus(now, Payment.Status.PENDING, PageRequest.of(0, batchSize))
                        .getContent();

                if (expiredPayments.isEmpty()) {
                    break;
                }

                for (Payment payment : expiredPayments) {
                    payment.setStatus(Payment.Status.FAILED);
                    reservationRepository.findById(payment.getReservationId()).ifPresentOrElse(reservation -> {
                        if (reservation.getStatus() == Status.PENDING) {
                            reservation.setStatus(Status.EXPIRED);
                            reservationRepository.save(reservation);
                        }
                    }, () -> log.warn("Reservation not found for paymentId={} reservationId={}",
                            payment.getId(), payment.getReservationId()));
                    paymentRepository.save(payment);
                    processed++;
                }

                if (expiredPayments.size() < batchSize) {
                    break;
                }
            }

            log.info("Expired payment cleanup processed {} records", processed);
        } finally {
            running.set(false);
        }
    }
}
