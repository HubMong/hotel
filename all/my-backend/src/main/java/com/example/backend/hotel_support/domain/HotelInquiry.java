package com.example.backend.hotel_support.domain;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.authlogin.domain.User; 
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotelInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🚨 [복구] DB 스키마에 존재하는 hotel_id 필드 복구 (NOT NULL 제약조건 위반 방지)
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId; // ⬅️ 이 필드가 컴파일 오류의 핵심 원인!

    // [기존 필드 유지] Service에서 Long userId를 저장하기 위해 필요합니다.
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 🚀 [추가] User 엔티티와의 관계 설정 (JPA 쿼리 경로 'i.user'를 위해 추가)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false) 
    private User user; 

    // 예약(Reservation) 엔티티와의 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false) 
    private Reservation reservation;

    // 객실(Room) 엔티티와의 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room; 

    @Column(nullable = false)
    private String title;
    
    @Lob
    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String status;

    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;

    @Builder
    public HotelInquiry(Long userId, Long hotelId, Room room, Reservation reservation, String title, String message, String status, LocalDateTime createdAt) {
        // 🚨 [수정] hotelId 인자를 Builder에 추가하여 Service 로직과 일치시킵니다.
        this.userId = userId;
        this.hotelId = hotelId; // ⬅️ 추가된 필드 매핑
        this.room = room; 
        this.reservation = reservation;
        this.title = title;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }
}