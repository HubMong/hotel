package com.example.backend.hotel_reservation.repository;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findTop500ByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);
    Page<Reservation> findByUserId(Long userId, Pageable pageable);
}
