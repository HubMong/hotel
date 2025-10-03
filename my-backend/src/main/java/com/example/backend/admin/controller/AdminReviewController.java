package com.example.backend.admin.controller;

import com.example.backend.admin.domain.Review;
import com.example.backend.admin.service.AdminReviewService;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.dto.ReviewDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminReviewController {

    private final AdminReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReviewDetailDto>>> list(
            @RequestParam(required = false) Boolean reported,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(required = false) String hotelName,
            @RequestParam(required = false) String userName,
            Pageable pageable) {
        log.info("리뷰 목록 API 호출 - reported: {}, hidden: {}, hotelName: {}, userName: {}", 
                reported, hidden, hotelName, userName);
        
        var page = reviewService.listWithDetails(reported, hidden, hotelName, userName, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewDetailDto>> detail(@PathVariable Long id) {
        try {
            log.info("리뷰 상세 조회 요청 - ID: {}", id);
            ReviewDetailDto detail = reviewService.getDetail(id);
            log.info("리뷰 상세 조회 완료 - ID: {}", id);
            return ResponseEntity.ok(ApiResponse.ok(detail));
        } catch (RuntimeException e) {
            log.error("리뷰 상세 조회 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("리뷰 상세 조회 중 예상치 못한 오류 - ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("서버 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/hide")
    public ResponseEntity<ApiResponse<Void>> hide(@PathVariable Long id) {
        try {
            reviewService.hide(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            log.error("리뷰 숨김 처리 실패 - ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("리뷰 숨김 처리에 실패했습니다."));
        }
    }

    @PutMapping("/{id}/show")
    public ResponseEntity<ApiResponse<Void>> show(@PathVariable Long id) {
        try {
            reviewService.show(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            log.error("리뷰 숨김 해제 실패 - ID: {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.fail("리뷰 숨김 해제에 실패했습니다."));
        }
    }

    // 신고 처리 기능 제거됨
}
