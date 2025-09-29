package com.example.backend.admin.controller;

import com.example.backend.admin.domain.Notice;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.service.AdminNoticeService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminNoticeController {
    private final AdminNoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Notice>>> list(@RequestParam(required = false) Boolean active, Pageable pageable) {
        var page = noticeService.list(active, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Notice>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.get(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Notice>> create(@RequestBody Notice n) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.create(n)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Notice>> update(@PathVariable Long id, @RequestBody Notice n) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.update(id, n)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
