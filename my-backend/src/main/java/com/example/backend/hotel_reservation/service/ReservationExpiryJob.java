package com.example.backend.hotel_reservation.service;

import com.example.backend.hotel_reservation.domain.*;
import com.example.backend.hotel_reservation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {
    private final ReservationRepository resRepo;
    private final RoomInventoryRepository invRepo;

    // 15초마다 만료체크
    @Scheduled(fixedDelay = 15_000)
    @Transactional
    public void expirePending() {
        Instant now = Instant.now();
        List<Reservation> list = resRepo.findTop500ByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now);
        for (Reservation r : list) {
            // 재고 복구
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
            r.setStatus(ReservationStatus.CANCELLED);
            resRepo.save(r);
            log.info("[EXPIRE] reservationId={} → CANCELLED", r.getId());
        }
    }
}
