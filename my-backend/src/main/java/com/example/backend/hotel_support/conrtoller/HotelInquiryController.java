package com.example.backend.hotel_support.controller;

import com.example.backend.hotel_support.dto.HotelInquiryRequest;
import com.example.backend.hotel_support.dto.HotelInquiryResponse;
import com.example.backend.hotel_support.service.HotelInquiryUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hotel-inquiries")
public class HotelInquiryController {

    private final HotelInquiryUserService hotelInquiryUserService;

    /**
     * @PostMapping: 사용자 문의 생성
     */
    @PostMapping
    public ResponseEntity<Void> createInquiry(
            // 🚨 [수정] SpEL 표현식 제거. Principal 객체 자체(String 이메일)를 Object로 주입받습니다.
            @AuthenticationPrincipal Object principal, 
            @RequestBody HotelInquiryRequest request
    ) {
        // Principal이 String(이메일)이라고 가정하고, null 또는 "anonymousUser"인지 확인하여 인증 체크
        if (principal == null || principal.toString().equals("anonymousUser")) {
            throw new SecurityException("로그인이 필요합니다."); 
        }
        
        String username = principal.toString(); 
        hotelInquiryUserService.createHotelInquiry(username, request);
        return ResponseEntity.ok().build();
    }

    /**
     * @GetMapping("/my"): 나의 문의 내역 조회
     */
    @GetMapping("/my")
    public ResponseEntity<List<HotelInquiryResponse>> getMyInquiries(
            // 🚨 [수정] SpEL 표현식 제거. Principal 객체 자체(String 이메일)를 Object로 주입받습니다.
            @AuthenticationPrincipal Object principal
    ) {
        // 널 체크 및 익명 사용자 체크
        if (principal == null || principal.toString().equals("anonymousUser")) {
            throw new SecurityException("로그인이 필요합니다.");
        }

        String username = principal.toString();
        List<HotelInquiryResponse> inquiries = hotelInquiryUserService.getInquiriesByUserId(username);
        return ResponseEntity.ok(inquiries);
    }
}