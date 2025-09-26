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
    private Integer numRooms; // NOT NULL (DB default 1) → @PrePersist로 보강

    @Column(name="num_adult", nullable=false)
    private Integer numAdult; // NOT NULL (DB default 0) → @PrePersist로 보강

    @Column(name="num_kid", nullable=false)
    private Integer numKid;   // NOT NULL (DB default 0) → @PrePersist로 보강

    @Column(name="start_date", nullable=false)
    private Instant startDate; // UTC 권장

    @Column(name="end_date", nullable=false)
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable=false)
    private ReservationStatus status; // PENDING/COMPLETED/CANCELLED → @PrePersist로 기본값 보강

    @Column(name="expires_at")
    private Instant expiresAt;

    @Column(name="transaction_id")
    private String transactionId;

    // ▼ 추가: 스키마에 created_at이 있다면 매핑 (없으면 이 필드/매핑 둘 다 삭제)
    @Column(name="created_at", updatable = false, insertable = false)
    private Instant createdAt;
    // ↑ DB에서 CURRENT_TIMESTAMP로 채워지도록 insertable=false로 두면 JPA가 값을 안 보냄.
    //   읽을 일만 있으면 이 방식이 가장 안전.

    // ▼ 추가: DB DEFAULT를 엔티티 레벨에서도 보장해주는 안전장치
    @PrePersist
    protected void onCreate() {
        if (numRooms == null) numRooms = 1;
        if (numAdult == null) numAdult = 0;
        if (numKid == null)   numKid   = 0;
        if (status == null)   status   = ReservationStatus.PENDING;
        // createdAt은 DB default를 쓰므로 건드리지 않음
    }
}
