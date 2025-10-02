// src/main/java/com/example/backend/hotel_support/service/OwnerInquiryService.java

package com.example.backend.hotel_support.service;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.service.HotelService; // ⭐ 주입받을 HotelService
import com.example.backend.authlogin.domain.User; // User 엔티티
import com.example.backend.admin.repository.UserRepository; // UserRepository

import com.example.backend.hotel_support.domain.HotelInquiry;
import com.example.backend.hotel_support.dto.HotelInquiryResponse;
import com.example.backend.hotel_support.repository.HotelInquiryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException; 
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OwnerInquiryService {

    private final HotelInquiryRepository inquiryRepository;
    private final HotelService hotelService; // ⭐ [수정] HotelService 필드 추가
    private final UserRepository userRepository; // UserRepository는 문의자 정보 조회용으로 유지
    
    // 문의 목록 조회 (오너가 소유한 호텔만 조회)
    public List<HotelInquiryResponse> getInquiriesByOwner(Long ownerId, Long filterHotelId, String filterStatus) {
        
        // 1단계: 오너 소유 호텔 ID 목록 및 이름 조회 (권한 범위)
        // HotelService의 getHotelsByOwner(ownerId)를 사용해야 HotelRepository 의존성 충돌을 피할 수 있습니다.
        List<Hotel> ownerHotels = hotelService.getHotelsByOwner(ownerId); 

        Map<Long, String> ownerHotelMap = ownerHotels.stream()
                .collect(Collectors.toMap(Hotel::getId, Hotel::getName));
        List<Long> ownerHotelIds = ownerHotels.stream().map(Hotel::getId).collect(Collectors.toList());

        if (ownerHotelIds.isEmpty()) return Collections.emptyList();

        // 2단계: 쿼리 대상 Hotel ID 설정 및 권한 검증
        List<Long> targetHotelIds;
        if (filterHotelId != null) {
             if (!ownerHotelIds.contains(filterHotelId)) {
                throw new SecurityException("접근 권한이 없는 호텔 ID입니다.");
            }
            targetHotelIds = Collections.singletonList(filterHotelId);
        } else {
            targetHotelIds = ownerHotelIds;
        }
        
        // 3단계: Repository 쿼리 실행
        List<HotelInquiry> inquiries;
        String status = filterStatus == null || filterStatus.equals("ALL") ? null : filterStatus;
        
        if (status != null) {
            inquiries = inquiryRepository.findByHotelIdInAndStatusOrderByCreatedAtDesc(targetHotelIds, status);
        } else {
            inquiries = inquiryRepository.findByHotelIdInOrderByCreatedAtDesc(targetHotelIds);
        }
        
        // 4단계: DTO 변환을 위해 필요한 User 데이터 조회 (UserRepository 사용)
        List<Long> userIds = inquiries.stream().map(HotelInquiry::getUserId).collect(Collectors.toList());
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                                            .collect(Collectors.toMap(User::getId, user -> user));

        // 5단계: 최종 Response DTO로 매핑
        return inquiries.stream()
                .map(inquiry -> new HotelInquiryResponse(
                    inquiry,
                    userMap.getOrDefault(inquiry.getUserId(), new User()).getName(),
                    ownerHotelMap.getOrDefault(inquiry.getHotelId(), "Unknown Hotel")
                ))
                .collect(Collectors.toList());
    }
    
    // 문의 답변 등록/수정
    @Transactional
    public void addReply(Long ownerId, Long inquiryId, String replyContent) {
        
        HotelInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NoSuchElementException("문의를 찾을 수 없습니다: " + inquiryId));

        // 1. 오너의 소유 호텔 확인 (권한 검증)
        // HotelService의 getHotel(id) 메서드를 사용
        Hotel hotel = hotelService.getHotel(inquiry.getHotelId());

        // ⭐ [수정] Hotel 엔티티에서 getOwner()를 통해 User 객체를 얻고, 그 객체의 ID와 비교
        // (Hotel 엔티티에 private User owner; 필드가 있다고 가정)
        if (!hotel.getOwner().getId().equals(ownerId)) {
            throw new SecurityException("해당 문의에 대한 답변 권한이 없습니다.");
        }
        
        // 2. 답변 내용 업데이트
        inquiry.setReplyContent(replyContent);
        inquiry.setRepliedAt(LocalDateTime.now());
        inquiry.setStatus("ANSWERED");
        
        // 트랜잭션 범위 내이므로 자동 저장됩니다.
    }
}