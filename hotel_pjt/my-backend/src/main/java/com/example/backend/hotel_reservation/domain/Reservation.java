package com.example.backend.hotel_reservation.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "Reservation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="room_id", nullable=false)
    private Long roomId;

    @Column(name="num_rooms", nullable=false)
    private Integer numRooms; // 추가된 컬럼

    @Column(name="num_adult", nullable=false)
    private Integer numAdult;

    @Column(name="num_kid", nullable=false)
    private Integer numKid;

    @Column(name="start_date", nullable=false)
    private Instant startDate; // UTC 권장

    @Column(name="end_date", nullable=false)
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private ReservationStatus status; // PENDING/COMPLETED/CANCELLED

    @Column(name="expires_at")
    private Instant expiresAt;

    @Column(name="transaction_id")
    private String transactionId;
}
