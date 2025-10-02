// src/main/java/com/example/backend/hotel_support/dto/HotelInquiryRequest.java

package com.example.backend.hotel_support.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 사용자로부터 호텔 문의를 접수받을 때 사용하는 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HotelInquiryRequest {
    
    // 사용자가 드롭다운에서 선택한 예약 ID
    private Long reservationId; 
    
    // 문의 제목
    private String title;
    
    // 문의 내용
    private String message;
    
    // userId는 백엔드에서 토큰을 통해 추출합니다.
}