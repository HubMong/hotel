package com.example.backend.hotel_support.dto;

import com.example.backend.hotel_support.domain.HotelInquiry;
import lombok.Getter;
import lombok.Setter;
// ⭐ [추가] Builder 패턴 및 AllArgsConstructor
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder // ⭐ Builder 패턴 추가
@NoArgsConstructor // Lombok의 기본 생성자
@AllArgsConstructor // Builder 사용을 위한 전체 필드 생성자
public class HotelInquiryResponse {

    private Long id;
    private Long hotelId;
    private String hotelName; 
    private String userName; 
    private String title;
    private String message;
    private String replyContent;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
    
    // 오너 페이지에서 사용되는 생성자 (기존 코드 유지)
    public HotelInquiryResponse(HotelInquiry inquiry, String userName, String hotelName) {
        this.id = inquiry.getId();
        this.hotelId = inquiry.getHotelId();
        this.title = inquiry.getTitle();
        this.message = inquiry.getMessage();
        this.replyContent = inquiry.getReplyContent();
        this.status = inquiry.getStatus();
        this.createdAt = inquiry.getCreatedAt();
        this.repliedAt = inquiry.getRepliedAt();
        this.userName = userName;
        this.hotelName = hotelName;
    }

    /**
     * ⭐ [추가] Entity를 DTO로 변환하는 정적 팩토리 메서드 (사용자 조회용)
     */
    public static HotelInquiryResponse fromEntity(HotelInquiry inquiry) {
        return HotelInquiryResponse.builder()
                .id(inquiry.getId())
                .hotelId(inquiry.getHotelId())
                .title(inquiry.getTitle())
                .message(inquiry.getMessage())
                .replyContent(inquiry.getReplyContent())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .repliedAt(inquiry.getRepliedAt())
                
                // 사용자 조회 시에는 JOIN 없이 엔티티 정보만 사용 (userName, hotelName은 null)
                .build();
    }
}