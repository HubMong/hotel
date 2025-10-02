// src/main/java/com/example/backend/hotel_support/service/HotelInquiryUserService.java

package com.example.backend.hotel_support.service;

import com.example.backend.hotel_support.domain.HotelInquiry;
import com.example.backend.hotel_support.domain.InquiryStatus;
import com.example.backend.hotel_support.dto.HotelInquiryRequest;
import com.example.backend.hotel_support.dto.HotelInquiryResponse; // DTO 재사용 가정
import com.example.backend.hotel_support.repository.HotelInquiryRepository;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.repository.ReservationRepository; 

import com.example.backend.HotelOwner.domain.Room; // Room 엔티티 경로 가정
import com.example.backend.HotelOwner.repository.RoomRepository; // Room Repository 경로 가정

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException; 
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelInquiryUserService {

    private final HotelInquiryRepository inquiryRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository; // RoomRepository 주입
    
    // 이전에 해결해야 했던 오류들:
    // 1. InquiryStatus를 import
    // 2. NoSuchElementException 사용
    // 3. Reservation 엔티티의 getRoomId() 메서드 사용 (Reservation.java 확인 완료)
    // 4. Room 엔티티의 getHotel().getId() 메서드 사용 (가장 일반적인 JPA 관계 가정)

    /**
     * 1. 사용자 호텔 문의 등록 (POST /api/inquiries/hotel)
     */
    @Transactional
    public void createHotelInquiry(Long userId, HotelInquiryRequest request) {
        
        // 1. 예약 정보 조회 및 유효성 검사
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("유효한 예약 정보를 찾을 수 없습니다. (ID: " + request.getReservationId() + ")"));

        // 2. Room ID를 사용하여 Room 엔티티 조회
        Long roomId = reservation.getRoomId(); 
        
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("문의 대상 객실 정보를 찾을 수 없습니다. (Room ID: " + roomId + ")"));

        // 3. Room 엔티티에서 Hotel ID 추출 (Room -> Hotel 관계를 통해 추출)
        // Room 엔티티가 getHotel() 메서드를 가지고 있다고 가정합니다.
        Long targetHotelId = room.getHotel().getId(); 

        // 4. 문의 엔티티 생성
        HotelInquiry inquiry = HotelInquiry.builder()
                .userId(userId)
                .hotelId(targetHotelId) 
                .reservationId(request.getReservationId())
                .title(request.getTitle())
                .message(request.getMessage())
                .status(InquiryStatus.PENDING.name()) // PENDING 문자열로 저장
                .createdAt(LocalDateTime.now())
                .build();
        
        // 5. 저장
        inquiryRepository.save(inquiry);
    }

    /**
     * 2. 사용자 문의 내역 조회 (GET /api/inquiries/my/hotel)
     */
    public List<HotelInquiryResponse> getInquiriesByUserId(Long userId) {
        
        // userId를 기준으로 문의 목록을 최신순으로 조회
        List<HotelInquiry> inquiries = inquiryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // DTO로 변환하여 반환
        return inquiries.stream()
                // fromEntity를 사용하여 엔티티 필드만 DTO로 변환 (JOIN 정보 제외)
                .map(HotelInquiryResponse::fromEntity) 
                .collect(Collectors.toList());
    }
}