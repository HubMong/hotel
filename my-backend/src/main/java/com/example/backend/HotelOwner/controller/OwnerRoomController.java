// src/main/java/com/example/backend/HotelOwner/controller/OwnerRoomController.java
package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.dto.OwnerRoomDto;
import com.example.backend.HotelOwner.dto.OwnerRoomDto.UpdateRequest;
import com.example.backend.HotelOwner.service.OwnerRoomService;
import com.example.backend.authlogin.config.JwtUtil;
import com.example.backend.authlogin.domain.User;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/owner/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://hwiyeong.shop"}, allowCredentials = "true")
public class OwnerRoomController {

    private final OwnerRoomService roomService;
    private final JwtUtil jwtUtil;

    /** 등록 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> registerRoom(
            @RequestParam("hotelId") Long hotelId,
            @RequestPart("roomRequest") OwnerRoomDto.RegisterRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws AccessDeniedException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        Long ownerId = null;
        if (details instanceof User u) {
            ownerId = u.getId();
        }
        if (ownerId == null) throw new AccessDeniedException("ownerId not found in security context");
        Long roomId = roomService.registerRoom(ownerId, hotelId, request, images);
        return ResponseEntity.ok(roomId);
    }

    /** 목록 */
    @GetMapping
    public ResponseEntity<List<OwnerRoomDto.ListResponse>> getMyRooms(
            @RequestParam("hotelId") Long hotelId
    ) throws AccessDeniedException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        Long ownerId = null;
        if (details instanceof User u) {
            ownerId = u.getId();
        }
        if (ownerId == null) throw new AccessDeniedException("ownerId not found in security context");
        return ResponseEntity.ok(roomService.getRoomsForOwner(ownerId, hotelId));
    }

    /** 상세 */
    @GetMapping("/{roomId:\\d+}") // ✅ 숫자 제약
    public ResponseEntity<OwnerRoomDto.DetailResponse> getRoomDetails(
            @PathVariable Long roomId
    ) throws AccessDeniedException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        Long ownerId = null;
        if (details instanceof User u) {
            ownerId = u.getId();
        }
        if (ownerId == null) throw new AccessDeniedException("ownerId not found in security context");
        return ResponseEntity.ok(roomService.getRoomDetails(ownerId, roomId));
    }

    /** 수정 */
    @PutMapping(value = "/{roomId:\\d+}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // ✅ 숫자 제약
    public ResponseEntity<Void> updateRoom(
            @PathVariable Long roomId,
            @RequestPart("roomRequest") UpdateRequest request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "hotelId", required = false) Long hotelIdIgnored
    ) throws AccessDeniedException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        Long ownerId = null;
        if (details instanceof User u) {
            ownerId = u.getId();
        }
        if (ownerId == null) throw new AccessDeniedException("ownerId not found in security context");
        roomService.updateRoom(ownerId, roomId, request, newImages);
        return ResponseEntity.ok().build();
    }

    /** 삭제 */
    @DeleteMapping("/{roomId:\\d+}") // ✅ 숫자 제약
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId
    ) throws AccessDeniedException {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        Long ownerId = null;
        if (details instanceof User u) {
            ownerId = u.getId();
        }
        if (ownerId == null) throw new AccessDeniedException("ownerId not found in security context");
        roomService.deleteRoom(ownerId, roomId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
