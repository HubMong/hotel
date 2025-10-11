// src/main/java/com/example/backend/admin/controller/AdminHotelController.java
package com.example.backend.admin.controller;

import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.HotelAdminDto;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.service.AdminHotelService;
import com.example.backend.admin.repository.AdminUserRepository;
import com.example.backend.HotelOwner.domain.Hotel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/hotels")
@RequiredArgsConstructor
@CrossOrigin(
    origins = "http://localhost:5173",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS }
)
public class AdminHotelController {

    private final AdminHotelService hotelService;
    private final AdminUserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HotelAdminDto>>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer minStar,
            @RequestParam(required = false) Hotel.ApprovalStatus status,
            Pageable pageable) {
        try {
            Page<HotelAdminDto> page = hotelService.listWithBusinessInfo(name, minStar, status, pageable);
            return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("호텔 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 상태별 숏컷
    @GetMapping("/pending")   public ResponseEntity<ApiResponse<PageResponse<HotelAdminDto>>> listPending(Pageable p){ return list(null,null,Hotel.ApprovalStatus.PENDING, p); }
    @GetMapping("/approved")  public ResponseEntity<ApiResponse<PageResponse<HotelAdminDto>>> listApproved(Pageable p){ return list(null,null,Hotel.ApprovalStatus.APPROVED,p); }
    @GetMapping("/rejected")  public ResponseEntity<ApiResponse<PageResponse<HotelAdminDto>>> listRejected(Pageable p){ return list(null,null,Hotel.ApprovalStatus.REJECTED,p); }
    @GetMapping("/suspended") public ResponseEntity<ApiResponse<PageResponse<HotelAdminDto>>> listSuspended(Pageable p){ return list(null,null,Hotel.ApprovalStatus.SUSPENDED,p); }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<HotelAdminDto>> detail(@PathVariable("id") Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(hotelService.get(id)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("호텔 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
        try {
            hotelService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("호텔 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id:\\d+}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable("id") Long id,
                                                     @RequestParam(required = false) String note) {
        try {
            Long adminUserId = null;
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User u) {
                adminUserId = userRepository.findByEmail(u.getUsername()).map(x -> x.getId()).orElse(null);
            }
            hotelService.approve(id, adminUserId, note);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("승인 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id:\\d+}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable("id") Long id,
                                                    @RequestParam(required = false) String reason) {
        try {
            hotelService.reject(id, reason);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("거부 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id:\\d+}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable("id") Long id,
                                                     @RequestParam(required = false) String reason) {
        try {
            hotelService.suspend(id, reason);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("정지 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 숏컷 경로로도 승인/거부/정지 지원 (FE가 /pending/{id}/approve처럼 부르는 경우)
    @PutMapping("/pending/{id:\\d+}/approve")  public ResponseEntity<ApiResponse<Void>> approve2 (@PathVariable("id") Long id, @RequestParam(required=false) String note){ return approve(id, note); }
    @PutMapping("/pending/{id:\\d+}/reject")   public ResponseEntity<ApiResponse<Void>> reject2  (@PathVariable("id") Long id, @RequestParam(required=false) String reason){ return reject(id, reason); }
    @PutMapping("/pending/{id:\\d+}/suspend")  public ResponseEntity<ApiResponse<Void>> suspend2 (@PathVariable("id") Long id, @RequestParam(required=false) String reason){ return suspend(id, reason); }

    @GetMapping("/status-counts")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> statusCounts() {
        try {
            return ResponseEntity.ok(ApiResponse.ok(hotelService.getStatusCounts()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("상태별 카운트 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/{id:\\d+}/rooms")
    public ResponseEntity<ApiResponse<java.util.List<com.example.backend.admin.dto.RoomResponse>>> rooms(@PathVariable("id") Long hotelId) {
        try {
            var dtos = hotelService.listRoomsByHotel(hotelId).stream()
                    .map(com.example.backend.admin.dto.RoomResponse::from)
                    .toList();
            return ResponseEntity.ok(ApiResponse.ok(dtos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("객실 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    public static record StatusUpdateRequest(String status, String reason) {}

    @PutMapping("/{id:\\d+}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable("id") Long id,
                                                          @RequestBody StatusUpdateRequest request) {
        try {
            String status = request.status();
            String reason = request.reason();
            switch (status) {
                case "APPROVED" -> {
                    Long adminUserId = null;
                    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User u) {
                        adminUserId = userRepository.findByEmail(u.getUsername()).map(x -> x.getId()).orElse(null);
                    }
                    hotelService.approve(id, adminUserId, reason);
                }
                case "REJECTED" -> hotelService.reject(id, reason);
                case "SUSPENDED" -> hotelService.suspend(id, reason);
                default -> { return ResponseEntity.badRequest().body(ApiResponse.fail("Invalid status: " + status)); }
            }
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("상태 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
