package com.example.backend.hotel_reservation.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "reservation") // 소문자로 통일 권장
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false)
    private Long userId;

    @Column(name="room_id", nullable=false)
    private Long roomId;

    @Column(name="num_rooms", nullable=false)
    private Integer numRooms;

    @Column(name="num_adult", nullable=false)
    private Integer numAdult;

    @Column(name="num_kid", nullable=false)
    private Integer numKid;

    @Column(name="start_date", nullable=false)
    private Instant startDate;

    @Column(name="end_date", nullable=false)
    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name="expires_at")
    private Instant expiresAt;

    @Column(name="transaction_id")
    private String transactionId;

    @Column(name="created_at", updatable = false, insertable = false)
    private Instant createdAt;

    public enum Status { PENDING, COMPLETED, CANCELLED, EXPIRED }
    
    @PrePersist
    protected void onCreate() {
        // NOT NULL 필드 기본값 설정
        if (numRooms == null || numRooms <= 0) numRooms = 1;
        if (numAdult == null) numAdult = 0;
        if (numKid == null)   numKid   = 0;
        if (status == null)   status = Status.PENDING;
        if (this.resStatus == null) this.resStatus = ResStatus.NOT_VISITED; // 추가 
    }
    
    public enum ResStatus {
        NOT_VISITED,   // 미방문 (기본값)
        CHECKED_IN,    // 체크인
        CHECKED_OUT    // 체크아웃
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "res_status", nullable = false) 
    private ResStatus resStatus = ResStatus.NOT_VISITED;
}
