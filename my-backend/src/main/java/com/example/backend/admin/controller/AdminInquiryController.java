// src/main/java/com/example/backend/admin/controller/AdminInquiryController.java
package com.example.backend.admin.controller;

import com.example.backend.admin.dto.AdminInquiryResponse;
import com.example.backend.website_support.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.example.backend.admin.dto.ReplyRequest;

@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminInquiryResponse>> getAllInquiries(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userName
    ) {
        // 👇 [수정] 서비스 호출 시 파라미터를 그대로 전달합니다.
        List<AdminInquiryResponse> inquiries = inquiryService.getAllInquiries(status, userName);
        return ResponseEntity.ok(inquiries);
    }
    // [추가] 답변 등록 API
    @PostMapping("/{inquiryId}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addReply(
            @PathVariable Long inquiryId,
            @RequestBody ReplyRequest replyRequest
    ) {
        inquiryService.addReply(inquiryId, replyRequest.getReplyContent());
        return ResponseEntity.ok().build(); // 성공 시 200 OK 응답
    }
}