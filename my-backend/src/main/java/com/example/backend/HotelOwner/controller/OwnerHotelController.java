package com.example.backend.HotelOwner.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.dto.AmenityDto;
import com.example.backend.HotelOwner.dto.DailySalesDto;
import com.example.backend.HotelOwner.dto.DashboardDto;
import com.example.backend.HotelOwner.dto.HotelDto;
import com.example.backend.HotelOwner.dto.RoomDto;
import com.example.backend.HotelOwner.dto.SalesChartRequestDto;
import com.example.backend.HotelOwner.service.AmenityService;
import com.example.backend.HotelOwner.service.FileStorageService;
import com.example.backend.HotelOwner.service.HotelService; // 오너용 서비스 타입
import com.example.backend.HotelOwner.service.RoomService;
import com.example.backend.authlogin.config.JwtUtil;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
// import com.example.backend.hotel_reservation.service.ReservationService; // ★ cancelByOwner 없으면 잠시 주석

@RestController("ownerHotelController")                 // ★ 빈 이름 고정 (FE 컨트롤러와 충돌 방지)
@RequestMapping("/api/owner/hotels")                    // ★ FE 경로와 분리
@RequiredArgsConstructor
@Slf4j
public class OwnerHotelController {

    private final HotelService hotelService;
    private final RoomService roomService;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;
    private final AmenityService amenityService;
    // private final ReservationService reservationService; // ★ cancelByOwner 구현 전이면 주석 처리

    // 편의시설 전체
    @GetMapping("/amenities")
    public ResponseEntity<List<AmenityDto>> getAllAmenities() {
        return ResponseEntity.ok(amenityService.getAllAmenities());
    }

    // 호텔 생성
    @PostMapping
    public ResponseEntity<HotelDto> createHotel(
            @RequestPart("hotel") HotelDto hotelDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("Authorization") String authHeader) {

        Long ownerId = getUserIdFromToken(authHeader);

        List<String> imageUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            imageUrls = files.stream().map(fileStorageService::store).collect(Collectors.toList());
        }

        // ★ HotelDto에 getAmenityIds()가 있어야 함 (아래 DTO 섹션 참고)
        Hotel savedHotel = hotelService.createHotel(hotelDto, imageUrls, hotelDto.getAmenityIds(), ownerId);
        return ResponseEntity.ok(toDto(savedHotel));
    }

    // 내 호텔 목록
    @GetMapping("/my")
    public ResponseEntity<List<HotelDto>> getMyHotels(@RequestHeader("Authorization") String authHeader) {
        log.info("1. [OwnerHotelController] /api/owner/hotels/my 호출");
        try {
            Long ownerId = getUserIdFromToken(authHeader);
            List<Hotel> hotels = hotelService.getHotelsByOwner(ownerId);
            List<HotelDto> hotelDtos = hotels.stream().map(HotelDto::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(hotelDtos);
        } catch (Exception e) {
            log.error("[OwnerHotelController] 내 호텔 조회 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 호텔 단건 조회 (오너 전용 경로)
    @GetMapping("/{id}")
    public ResponseEntity<HotelDto> getHotel(@PathVariable Long id) {
        Hotel hotel = hotelService.getHotel(id);
        return ResponseEntity.ok(HotelDto.fromEntity(hotel));
    }

    // 호텔 수정
    @PostMapping("/{id}")
    public ResponseEntity<HotelDto> updateHotel(
            @PathVariable Long id,
            @RequestPart("hotel") HotelDto hotelDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("Authorization") String authHeader) {

        log.info("1. [OwnerHotelController-수정] /api/owner/hotels/{} 호출", id);
        Long ownerId = getUserIdFromToken(authHeader);

        Hotel existingHotel = hotelService.getHotel(id);
        if (!existingHotel.getOwner().getId().equals(ownerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<String> imageUrls = new ArrayList<>();
        if (hotelDto.getImageUrls() != null) {
            imageUrls.addAll(hotelDto.getImageUrls());
        }
        if (files != null && !files.isEmpty()) {
            imageUrls.addAll(files.stream().map(fileStorageService::store).toList());
        }

        HotelDto updatedHotelDto = hotelService.updateHotel(id, hotelDto, imageUrls, hotelDto.getAmenityIds());
        log.info("5. [OwnerHotelController-수정] 완료, 호텔 ID: {}", updatedHotelDto.getId());
        return ResponseEntity.ok(updatedHotelDto);
    }

    // 호텔 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        log.info("1. [OwnerHotelController-삭제] /api/owner/hotels/{} 호출", id);
        try {
            Long ownerId = getUserIdFromToken(authHeader);
            Hotel existingHotel = hotelService.getHotel(id);
            if (!existingHotel.getOwner().getId().equals(ownerId)) {
                log.warn("권한 없음. 소유주={}, 요청자={}", existingHotel.getOwner().getId(), ownerId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            hotelService.deleteHotel(id);
            log.info("4. [OwnerHotelController-삭제] 완료, 호텔 ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("[OwnerHotelController-삭제] 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 호텔의 객실 목록
    @GetMapping("/{hotelId}/rooms")
    public ResponseEntity<List<RoomDto>> getRoomsByHotel(@PathVariable Long hotelId) {
        List<RoomDto> roomDtos = roomService.findByHotelId(hotelId);
        return ResponseEntity.ok(roomDtos);
    }

    // 객실 생성
    @PostMapping("/{hotelId}/rooms")
    public ResponseEntity<RoomDto> createRoom(
            @PathVariable Long hotelId,
            @RequestPart("room") RoomDto roomDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<String> imageUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            imageUrls = files.stream().map(fileStorageService::store).collect(Collectors.toList());
        }

        RoomDto newRoomDto = roomService.createRoom(hotelId, roomDto, imageUrls, userDetails.getUsername());
        return ResponseEntity.ok(newRoomDto);
    }

    // 객실 수정
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<RoomDto> updateRoom(
            @PathVariable Long roomId,
            @RequestPart("room") RoomDto roomDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<String> imageUrls = new ArrayList<>();
        if (roomDto.getImageUrls() != null) imageUrls.addAll(roomDto.getImageUrls());
        if (files != null && !files.isEmpty()) {
            imageUrls.addAll(files.stream().map(fileStorageService::store).toList());
        }

        Room updatedRoom = roomService.updateRoom(roomId, roomDto, imageUrls, userDetails.getUsername());
        return ResponseEntity.ok(RoomDto.fromEntity(updatedRoom));
    }

    // 객실 삭제
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        roomService.deleteRoom(roomId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // 오너 대시보드
    @GetMapping("/dashboard/sales-summary")
    public ResponseEntity<DashboardDto> getSalesSummary(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        DashboardDto summary = hotelService.getSalesSummary(ownerId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/dashboard/daily-sales")
    public ResponseEntity<List<DailySalesDto>> getDailySales(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SalesChartRequestDto requestDto) {
        Long ownerId = getUserIdFromToken(authHeader);
        List<DailySalesDto> dailySales = hotelService.getDailySales(ownerId, requestDto);
        return ResponseEntity.ok(dailySales);
    }

    @GetMapping("/dashboard/activity")
    public ResponseEntity<ReservationDtos.DashboardActivityResponse> getDashboardActivity(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromToken(authHeader);
        return ResponseEntity.ok(hotelService.getDashboardActivity(ownerId));
    }

    // ===== util =====
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효하지 않은 인증 헤더입니다.");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.extractAllClaims(token);
        Object userIdObj = claims.get("userId");
        if (userIdObj == null) throw new IllegalArgumentException("토큰에 userId가 존재하지 않습니다.");

        if (userIdObj instanceof Integer i) return i.longValue();
        if (userIdObj instanceof Long l) return l;
        return Long.parseLong(userIdObj.toString());
    }

    private HotelDto toDto(Hotel hotel) {
        HotelDto dto = new HotelDto();
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setAddress(hotel.getAddress());
        dto.setStarRating(hotel.getStarRating());
        dto.setDescription(hotel.getDescription());
        dto.setCountry(hotel.getCountry());
        dto.setStatus(hotel.getApprovalStatus().name());
        if (hotel.getImages() != null) {
            dto.setImageUrls(hotel.getImages().stream().map(img -> img.getUrl()).toList());
        }
        if (hotel.getHotelAmenities() != null) {
            dto.setAmenityIds(hotel.getHotelAmenities().stream().map(ha -> ha.getAmenity().getId()).toList());
        }
        return dto;
    }
}
