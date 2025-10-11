package com.example.backend.admin.controller;

import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.dto.RoomCreateRequest;
import com.example.backend.admin.dto.RoomResponse;
import com.example.backend.admin.service.AdminRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {
	private final AdminRoomService roomService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<RoomResponse>>> list(@RequestParam(required = false) Long hotelId,
						@RequestParam(required = false) String name,
						Pageable pageable) {
		Page<RoomResponse> page = roomService.list(hotelId, name, pageable);
		return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
	}

	// Helper: simple list by hotel for admin UI convenience (query param)
	@GetMapping("/by-hotel")
	public ResponseEntity<ApiResponse<java.util.List<RoomResponse>>> listByHotel(@RequestParam Long hotelId) {
		var page = roomService.list(hotelId, null, org.springframework.data.domain.PageRequest.of(0, 1000));
		return ResponseEntity.ok(ApiResponse.ok(page.getContent()));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<RoomResponse>> get(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(roomService.get(id)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<RoomResponse>> create(@RequestBody RoomCreateRequest request) {
		RoomResponse saved = roomService.create(request);
		return ResponseEntity.ok(ApiResponse.ok(saved));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		try {
			roomService.delete(id);
			return ResponseEntity.ok(ApiResponse.ok(null));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.fail("객실 삭제 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<RoomResponse>> update(@PathVariable Long id, @RequestBody RoomCreateRequest request) {
		RoomResponse updated = roomService.update(id, request);
		return ResponseEntity.ok(ApiResponse.ok(updated));
	}

	// 객실 상태만 변경하는 전용 엔드포인트
	@PutMapping("/{id}/status")
	public ResponseEntity<ApiResponse<RoomResponse>> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
		if (request == null || request.status == null || request.status.isBlank()) {
			return ResponseEntity.badRequest().body(ApiResponse.fail("status 값이 필요합니다."));
		}
		RoomResponse updated = roomService.updateStatus(id, request.status);
		return ResponseEntity.ok(ApiResponse.ok(updated));
	}

	public static class StatusRequest {
		public String status;
		public String getStatus() { return status; }
		public void setStatus(String status) { this.status = status; }
	}
}