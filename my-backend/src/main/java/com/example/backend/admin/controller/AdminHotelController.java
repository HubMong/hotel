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
@CrossOrigin(origins = "http://localhost:5173")
public class AdminHotelController {

    private final AdminHotelService hotelService;
    private final AdminUserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<HotelAdminDto>>> list(@RequestParam(required = false) String name,
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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelAdminDto>> detail(@PathVariable Long id) {
        try {
            HotelAdminDto hotel = hotelService.get(id);
            return ResponseEntity.ok(ApiResponse.ok(hotel));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("호텔 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            hotelService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("호텔 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        try {
            Long adminUserId = null;
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
                String email = user.getUsername();
                var userOpt = userRepository.findByEmail(email);
                adminUserId = userOpt.map(u -> u.getId()).orElse(null);
            }

            // 인증되지 않은 경우에도 시스템 승인으로 처리
            hotelService.approve(id, adminUserId, note);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("승인 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            hotelService.reject(id, reason);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("거부 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            hotelService.suspend(id, reason);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("정지 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/status-counts")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> statusCounts() {
        try {
            java.util.Map<String, Long> counts = hotelService.getStatusCounts();
            return ResponseEntity.ok(ApiResponse.ok(counts));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("상태별 카운트 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // Admin: 호텔별 객실 목록 조회 (RoomResponse DTO로 반환 - 이미지 포함)
    @GetMapping("/{id}/rooms")
    public ResponseEntity<ApiResponse<java.util.List<com.example.backend.admin.dto.RoomResponse>>> rooms(@PathVariable("id") Long hotelId) {
        try {
            var rooms = hotelService.listRoomsByHotel(hotelId);
            
            // RoomResponse를 사용하여 이미지 정보 포함
            var dtos = rooms.stream()
                    .map(com.example.backend.admin.dto.RoomResponse::from)
                    .toList();
            return ResponseEntity.ok(ApiResponse.ok(dtos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("객실 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    public static record RoomSummaryDto(
        Long id,
        String name,
        String roomSize,
        Integer capacityMin,
        Integer capacityMax,
        String status,
        Integer price,
        Integer originalPrice,
        Boolean wifi,
        Boolean aircon,
        Boolean freeWater,
        Boolean hasWindow,
        Boolean sharedBath,
        Boolean smoke,
        String bed,
        Integer bath,
        String viewName,
        String cancelPolicy,
        String payment
    ) {
        public static RoomSummaryDto from(com.example.backend.HotelOwner.domain.Room r) {
            return new RoomSummaryDto(
                r.getId(),
                r.getName(),
                r.getRoomSize(),
                r.getCapacityMin(),
                r.getCapacityMax(),
                r.getStatus(),
                r.getPrice(),
                r.getOriginalPrice(),
                r.getWifi(),
                r.getAircon(),
                r.getFreeWater(),
                r.getHasWindow(),
                r.getSharedBath(),
                r.getSmoke(),
                r.getBed(),
                r.getBath(),
                r.getViewName(),
                r.getCancelPolicy(),
                r.getPayment()
            );
        }
    }
    
    public static record StatusUpdateRequest(String status, String reason) {}
    
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        try {
            String status = request.status();
            String reason = request.reason();

            switch (status) {
                case "APPROVED":
                    Long adminUserId = null;
                    var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) auth.getPrincipal();
                        String email = user.getUsername();
                        var userOpt = userRepository.findByEmail(email);
                        adminUserId = userOpt.map(u -> u.getId()).orElse(null);
                    }

                    // 인증되지 않은 경우에도 시스템 승인으로 처리
                    hotelService.approve(id, adminUserId, reason);
                    break;
                case "REJECTED":
                    hotelService.reject(id, reason);
                    break;
                case "SUSPENDED":
                    hotelService.suspend(id, reason);
                    break;
                default:
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Invalid status: " + status));
            }

            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.fail("상태 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
