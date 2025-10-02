// src/main/java/com/example/backend/hotel_support/domain/HotelInquiry.java

package com.example.backend.hotel_support.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder; // Lombok 필수는 유지
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "hotel_inquiry")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // ⭐ [필수 추가] 문의가 연결된 예약 ID
    @Column(nullable = false)
    private Long reservationId;
    
    @Column(nullable = false)
    private Long hotelId; // 문의가 접수된 호텔 ID
    
    @Column(nullable = false)
    private Long userId; // 문의를 보낸 사용자 ID

    @Column(nullable = false)
    private String title;

    @Lob 
    @Column(nullable = false)
    private String message;

    // ⭐ [수정 핵심] @Enumerated 어노테이션 제거. String 타입 유지.
    @Column(nullable = false) 
    private String status = "PENDING"; // PENDING, ANSWERED (String 타입)

    @Lob
    private String replyContent; // 오너의 답변 내용

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); 

    private LocalDateTime repliedAt;
}