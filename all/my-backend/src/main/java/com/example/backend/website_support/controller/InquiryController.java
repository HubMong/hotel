package com.example.backend.website_support.controller;

import com.example.backend.admin.repository.InquiryUserRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.website_support.domain.WebsiteInquiry;
import com.example.backend.website_support.dto.CreateInquiryRequest;
import com.example.backend.website_support.dto.InquiryResponse;
import com.example.backend.website_support.service.InquiryService;

import com.example.backend.hotel_support.dto.HotelInquiryRequest; 
import com.example.backend.hotel_support.dto.HotelInquiryResponse;
import com.example.backend.hotel_support.service.HotelInquiryUserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class InquiryController {

    private final InquiryService inquiryService;
    private final HotelInquiryUserService hotelInquiryUserService;
    private final InquiryUserRepository userRepository;

    public InquiryController(InquiryService inquiryService, 
                             HotelInquiryUserService hotelInquiryUserService,
                             InquiryUserRepository userRepository) {
        this.inquiryService = inquiryService;
        this.hotelInquiryUserService = hotelInquiryUserService;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
    
    // =========================================================
    // 1. 웹사이트 문의 API (수정 없음)
    // =========================================================
    @PostMapping
    public ResponseEntity<String> createInquiry(@RequestBody CreateInquiryRequest dto, @AuthenticationPrincipal String email) {
        User user = getUserByEmail(email);
        inquiryService.createInquiry(dto, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body("문의가 성공적으로 접수되었습니다.");
    }

    @GetMapping("/my")
    public ResponseEntity<List<InquiryResponse>> getMyInquiries(@AuthenticationPrincipal String email) {
        User user = getUserByEmail(email);
        List<InquiryResponse> myInquiries = inquiryService.getMyInquiries(user.getId());
        return ResponseEntity.ok(myInquiries);
    }
    
    @GetMapping("/{inquiryId}")
    public ResponseEntity<WebsiteInquiry> getInquiryById(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal String email) {
        
        User user = getUserByEmail(email);
        WebsiteInquiry inquiry = inquiryService.getInquiryByIdForUser(inquiryId, user.getId());
        return ResponseEntity.ok(inquiry);
    }

    // =========================================================
    // 2. 호텔 문의 API (⭐ 수정된 부분 ⭐)
    // =========================================================

    /**
     * POST /api/inquiries/hotel : 사용자 호텔 문의 접수
     */
    @PostMapping("/hotel")
    public ResponseEntity<String> submitHotelInquiry(
        @RequestBody HotelInquiryRequest request,
        @AuthenticationPrincipal String email 
    ) {
        // 🔽 user.getId() 대신 email(String)을 그대로 전달합니다.
        hotelInquiryUserService.createHotelInquiry(email, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body("호텔 문의가 성공적으로 접수되었습니다.");
    }

    /**
     * GET /api/inquiries/my/hotel : 사용자 본인의 호텔 문의 내역 조회
     */
    @GetMapping("/my/hotel")
    public ResponseEntity<List<HotelInquiryResponse>> getMyHotelInquiries(
        @AuthenticationPrincipal String email 
    ) {
        // 🔽 user.getId() 대신 email(String)을 그대로 전달합니다.
        List<HotelInquiryResponse> inquiries = hotelInquiryUserService.getInquiriesByUserId(email);
        return ResponseEntity.ok(inquiries);
    }
}