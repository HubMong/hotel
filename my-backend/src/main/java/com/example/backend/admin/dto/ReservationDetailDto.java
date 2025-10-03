package com.example.backend.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReservationDetailDto {
    // 예약 기본 정보
    private Long reservationId;
    private String transactionId;
    private Integer numAdult;
    private Integer numKid;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;
    private String reservationStatus;
    private LocalDateTime expiresAt;
    
    // 호텔 정보
    private Long hotelId;
    private String hotelName;
    private String hotelAddress;
    private Integer starRating;
    
    // 객실 정보
    private Long roomId;
    private String roomName;
    private String roomType; // roomSize를 타입으로 사용
    private Integer capacityMin;
    private Integer capacityMax;
    private Integer roomPrice;
    
    // 사용자 정보
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    
    // 결제 정보
    private Long paymentId;
    private String paymentMethod;
    private Integer basePrice;
    private Integer totalPrice;
    private Integer tax;
    private Integer discount;
    private String paymentStatus;
    private LocalDateTime paymentCreatedAt;
    private String receiptUrl;
    
    // 편의용 메서드
    public Integer getTotalGuests() {
        return (numAdult != null ? numAdult : 0) + (numKid != null ? numKid : 0);
    }
}
