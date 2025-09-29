package com.example.backend.admin.controller;

import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.dto.PageResponse;
import com.example.backend.admin.service.AdminRoomService;
import com.example.backend.HotelOwner.domain.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class RoomController {
	private final AdminRoomService roomService;

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<Room>>> list(@RequestParam(required = false) Long hotelId,
								@RequestParam(required = false) String name,
								Pageable pageable) {
		Page<Room> page = roomService.list(hotelId, name, pageable);
		return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Room>> get(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.ok(roomService.get(id)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		roomService.delete(id);
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}