package com.example.backend.hotel_reservation.service;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.RoomInventory;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.hotel_reservation.repository.RoomInventoryRepository;
import com.example.backend.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {
    private final ReservationRepository resRepo;
    private final RoomInventoryRepository invRepo;
    private final PaymentRepository paymentRepo;

    @Value("${reservation.expiry.batch-size:200}")
    private int expiryBatchSize;

    @Value("${reservation.expiry.max-iterations:3}")
    private int maxIterationsPerRun;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${reservation.expiry.fixed-delay-ms:60000}")
    @Transactional
    public void expirePending() {
        if (!running.compareAndSet(false, true)) {
            log.debug("Skip expirePending run because previous execution is still active");
            return;
        }

        try {
            Instant now = Instant.now();
            int iteration = 0;
            while (iteration++ < maxIterationsPerRun) {
                List<Reservation> batch = resRepo
                        .findByStatusAndExpiresAtBefore(Reservation.Status.PENDING, now,
                                PageRequest.of(0, expiryBatchSize))
                        .getContent();

                if (batch.isEmpty()) {
                    break;
                }

                processBatch(batch);

                if (batch.size() < expiryBatchSize) {
                    break;
                }
            }
        } catch (Exception ex) {
            log.error("Failed to expire pending reservations", ex);
        } finally {
            running.set(false);
        }
    }

    private void processBatch(List<Reservation> reservations) {
        for (Reservation r : reservations) {
            if (paymentRepo.findByReservationId(r.getId()).isPresent()) {
                continue;
            }

            LocalDate ci = r.getStartDate().atZone(java.time.ZoneOffset.UTC).toLocalDate();
            LocalDate co = r.getEndDate().atZone(java.time.ZoneOffset.UTC).toLocalDate();
            int qty = r.getNumRooms() == null ? 1 : r.getNumRooms();

            for (LocalDate d = ci; d.isBefore(co); d = d.plusDays(1)) {
                RoomInventory inv = invRepo.findWithLock(r.getRoomId(), d).orElse(null);
                if (inv != null) {
                    inv.setAvailableQuantity(inv.getAvailableQuantity() + qty);
                    invRepo.save(inv);
                }
            }
            r.setStatus(Reservation.Status.EXPIRED);
            resRepo.save(r);
            log.info("[EXPIRE] reservationId={} → EXPIRED", r.getId());
        }
    }
}
