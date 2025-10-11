package com.example.backend.admin.controller;

import com.example.backend.admin.domain.Inquiry;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.service.AdminCSService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cs")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminCSController {
    private final AdminCSService csService;

    @GetMapping("/inquiries")
    public ResponseEntity<ApiResponse<PageResponse<Inquiry>>> listInquiries(@RequestParam(required = false) Inquiry.Status status,
                                                                            Pageable pageable) {
        var page = csService.listInquiries(status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
    }

    public static record ReplyRequest(String reply) {}

    @PutMapping("/inquiries/{id}/reply")
    public ResponseEntity<ApiResponse<Inquiry>> reply(@PathVariable Long id, @RequestBody ReplyRequest req) {
        var updated = csService.reply(id, req.reply());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}
