package com.example.backend.hotel_support.controller;

import com.example.backend.hotel_support.dto.HotelInquiryResponse;
import com.example.backend.hotel_support.dto.ReplyRequest;
import com.example.backend.hotel_support.service.OwnerInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/inquiries")
public class HotelInquiryOwnerController {

    private final OwnerInquiryService ownerService;

    /**
     * @GetMapping: 점주 소유 호텔 문의 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<HotelInquiryResponse>> getInquiries(
            @RequestParam List<Long> hotelIds,
            @AuthenticationPrincipal Object principal 
    ) {
        if (principal == null || principal.toString().equals("anonymousUser")) {
            throw new SecurityException("로그인이 필요합니다."); 
        }
        
        String userName = principal.toString();
        
        // 🔑 [수정] findInquiriesForOwner 호출 (이전에 객실명과 이메일 로직이 포함된 것으로 가정)
        List<HotelInquiryResponse> inquiries = ownerService.findInquiriesForOwner(hotelIds, userName); 
        return ResponseEntity.ok(inquiries);
    }

    /**
     * @PostMapping: 문의에 대한 답변 등록
     */
    @PostMapping("/{id}/reply")
    public ResponseEntity<Void> submitReply(
            @PathVariable("id") Long inquiryId,
            @RequestBody ReplyRequest request,
            @AuthenticationPrincipal Object principal 
    ) { 
        if (principal == null || principal.toString().equals("anonymousUser")) {
            throw new SecurityException("관리자 권한이 필요합니다."); 
        }
        
        String ownerEmail = principal.toString(); 
        ownerService.addReply(inquiryId, request.getReplyContent(), ownerEmail); 
        return ResponseEntity.ok().build();
    }
    
    /**
     * 🔑 [추가] 미답변 문의 개수 반환 (알림 기능 활성화)
     * 엔드포인트: GET /api/owner/inquiries/unanswered-count
     */
    @GetMapping("/unanswered-count")
    public ResponseEntity<Long> getUnansweredInquiryCount(
        // 프론트엔드 OwnerMain.vue에서 점주 소유 호텔 ID 목록을 받습니다.
        @RequestParam List<Long> hotelIds 
    ) {
        // 🔑 Service 메서드를 호출하여 미답변 문의 개수를 반환합니다.
        long count = ownerService.countPendingInquiries(hotelIds);
        return ResponseEntity.ok(count);
    }
}