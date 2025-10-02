package com.example.backend.admin.controller;

import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.HotelAdminDto;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.repository.UserRepository;
import com.example.backend.admin.service.AdminHotelService;
import com.example.backend.HotelOwner.domain.Hotel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/hotels")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminHotelController {

    private final AdminHotelService hotelService;
    private final UserRepository userRepository;

    /** 목록: DB 정렬/페이징(기본 createdAt DESC), 예외는 전역 핸들러가 처리 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HotelAdminDto>>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minStar,
            @RequestParam(required = false) Hotel.ApprovalStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<HotelAdminDto> page = hotelService.list(name, minStar, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelAdminDto>> detail(@PathVariable Long id) {
        HotelAdminDto hotel = hotelService.get(id);
        return ResponseEntity.ok(ApiResponse.ok(hotel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        hotelService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id,
                                                     @RequestParam(required = false) String note) {
        Long adminUserId = resolveAdminUserIdOrNull();
        hotelService.approve(id, adminUserId, note);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Long id,
                                                    @RequestParam(required = false) String reason) {
        hotelService.reject(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable Long id,
                                                     @RequestParam(required = false) String reason) {
        hotelService.suspend(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public static record StatusUpdateRequest(String status, String reason) {}

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id,
                                                          @RequestBody StatusUpdateRequest request) {
        String status = request.status();
        String reason = request.reason();

        switch (status) {
            case "APPROVED" -> {
                Long adminUserId = resolveAdminUserIdOrNull();
                hotelService.approve(id, adminUserId, reason);
            }
            case "REJECTED" -> hotelService.reject(id, reason);
            case "SUSPENDED" -> hotelService.suspend(id, reason);
            default -> {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Invalid status: " + status));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /** 반복되는 관리자 ID 추출 로직을 헬퍼로 통일 */
    private Long resolveAdminUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        // 스프링 시큐리티 기본 UserDetails
        if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
            String email = userDetails.getUsername();
            return userRepository.findByEmail(email).map(u -> u.getId()).orElse(null);
        }
        // 커스텀 프린시펄 등 다른 타입이면 여기서 추가 분기 가능
        return null;
    }
}
