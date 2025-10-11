package com.example.backend.hotel_support.dto;

import com.example.backend.hotel_support.domain.HotelInquiry;
import com.example.backend.hotel_support.domain.InquiryStatus; 
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
public class HotelInquiryResponse {
    
    private final Long id;
    private final String title;

    private final String message; 
    private final String status;
    private final String date;
    
    private final Long hotelId;
    private final Long reservationId;
    
    private final String userName;
    private final String hotelName;
    private final String roomName; // 🔑 [추가] 객실명 필드
    private final String adminReply;     
    
    /**
     * 사용자용 생성자 (3개 인자 생성자의 호환성을 위해 roomName을 null로 설정)
     */
    private HotelInquiryResponse(HotelInquiry inquiry) {
        this.id = inquiry.getId();
        this.title = inquiry.getTitle();
        this.message = inquiry.getMessage(); 
        this.adminReply = inquiry.getAdminReply();
        this.roomName = null; // 🔑 [수정] final 필드 초기화
        
        // 상태 변환 로직 유지
        this.status = InquiryStatus.valueOf(inquiry.getStatus()).getDisplayValue();
        
        if (inquiry.getCreatedAt() != null) {
            this.date = inquiry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            this.date = "정보 없음";
        }
        
        this.hotelId = inquiry.getRoom() != null && inquiry.getRoom().getHotel() != null
                         ? inquiry.getRoom().getHotel().getId() 
                         : null; 
        this.reservationId = inquiry.getReservation() != null 
                          ? inquiry.getReservation().getId() 
                          : null;
        
        this.userName = null;
        this.hotelName = null;
    }

    // 🔑 [손상 복구용] 3개 인자 생성자 오버로딩 (기존 코드를 손상시키지 않고 호환성 유지)
    public HotelInquiryResponse(HotelInquiry inquiry, String userName, String hotelName) {
        // 4개 인자 생성자에게 roomName을 null로 전달하여 호환성 확보
        this(inquiry, userName, hotelName, null); 
    }

    /**
     * 점주용 최종 생성자 (객실명 인자를 받도록 변경)
     */
    public HotelInquiryResponse(HotelInquiry inquiry, String userName, String hotelName, String roomName) { // 🔑 4개 인자
        this.id = inquiry.getId();
        this.title = inquiry.getTitle();
        this.message = inquiry.getMessage(); 
        this.adminReply = inquiry.getAdminReply();
        this.roomName = roomName; // 🔑 [할당] roomName 인자 할당
        
        // 상태 변환 로직 유지
        this.status = InquiryStatus.valueOf(inquiry.getStatus()).getDisplayValue();
        
        // 🚀 [수정] Invalid Date 오류 해결: 시간 정보를 포함하여 정확한 포맷을 사용합니다.
        if (inquiry.getCreatedAt() != null) {
            this.date = inquiry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else {
            this.date = "정보 없음";
        }
        
        // ID 매핑 (널 방어 유지)
        this.hotelId = inquiry.getRoom() != null && inquiry.getRoom().getHotel() != null
                         ? inquiry.getRoom().getHotel().getId() 
                         : null; 
        this.reservationId = inquiry.getReservation() != null 
                          ? inquiry.getReservation().getId() 
                          : null;
        
        this.userName = userName;
        this.hotelName = hotelName;
    }

    public static HotelInquiryResponse fromEntity(HotelInquiry inquiry) {
        return new HotelInquiryResponse(inquiry);
    }
}
