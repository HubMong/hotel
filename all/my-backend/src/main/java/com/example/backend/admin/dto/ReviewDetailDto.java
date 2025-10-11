package com.example.backend.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewDetailDto {
    // 리뷰 기본 정보
    private Long reviewId;
    private Integer starRating;
    private String content;
    private String image;
    private LocalDateTime wroteOn;
    
    // 예약 정보
    private Long reservationId;
    private String transactionId;
    
    // 호텔 정보
    private Long hotelId;
    private String hotelName;
    
    // 사용자 정보
    private Long userId;
    private String userName;
    private String userEmail;
    
    // 관리자용 필드
    private Boolean isReported;
    private Boolean isHidden;
    private String adminReply;
    private LocalDateTime repliedAt;
}
