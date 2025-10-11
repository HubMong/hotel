// src/main/java/com/example/backend/HotelOwner/controller/OwnerRoomController.java
package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.dto.OwnerRoomDto;
import com.example.backend.HotelOwner.dto.OwnerRoomDto.UpdateRequest;
import com.example.backend.HotelOwner.service.OwnerRoomService;
import com.example.backend.authlogin.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/owner/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class OwnerRoomController {

    private final OwnerRoomService roomService;
    private final JwtUtil jwtUtil;

    /** 등록 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> registerRoom(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("hotelId") Long hotelId,
            @RequestPart("roomRequest") OwnerRoomDto.RegisterRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        Long roomId = roomService.registerRoom(ownerId, hotelId, request, images);
        return ResponseEntity.ok(roomId);
    }

    /** 목록 */
    @GetMapping
    public ResponseEntity<List<OwnerRoomDto.ListResponse>> getMyRooms(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("hotelId") Long hotelId
    ) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        return ResponseEntity.ok(roomService.getRoomsForOwner(ownerId, hotelId));
    }

    /** 상세 */
    @GetMapping("/{roomId}")
    public ResponseEntity<OwnerRoomDto.DetailResponse> getRoomDetails(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long roomId
    ) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        return ResponseEntity.ok(roomService.getRoomDetails(ownerId, roomId));
    }

    /** 수정 */
    @PutMapping(value = "/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateRoom(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long roomId,
            @RequestPart("roomRequest") UpdateRequest request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "hotelId", required = false) Long hotelIdIgnored // 프런트 쿼리 보호용
    ) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        roomService.updateRoom(ownerId, roomId, request, newImages);
        return ResponseEntity.ok().build();
    }

    /** 삭제 */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long roomId
    ) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        roomService.deleteRoom(ownerId, roomId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
