package com.example.backend.hotel_reservation.repository;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 만료된 PENDING 예약 조회
    List<Reservation> findTop500ByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);

    // 특정 사용자 예약 조회
    List<Reservation> findByUserId(Long userId);
}
