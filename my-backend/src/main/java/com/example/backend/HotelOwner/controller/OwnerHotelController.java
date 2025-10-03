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

@RestController("ownerHotelController")
@RequestMapping("/api/owner/hotels")
@RequiredArgsConstructor
@Slf4j
public class OwnerHotelController {

    private final HotelService hotelService;
    private final RoomService roomService;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService;
    private final AmenityService amenityService;

    @GetMapping("/amenities")
    public ResponseEntity<List<AmenityDto>> getAllAmenities() {
        return ResponseEntity.ok(amenityService.getAllAmenities());
    }

    @PostMapping
    public ResponseEntity<HotelDto> createHotel(
            @RequestPart("hotel") HotelDto hotelDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("Authorization") String authHeader) {

        Long ownerId = getUserIdFromTokenLenient(authHeader);

        List<String> imageUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            imageUrls = files.stream().map(fileStorageService::store).collect(Collectors.toList());
        }

        Hotel savedHotel = hotelService.createHotel(hotelDto, imageUrls, hotelDto.getAmenityIds(), ownerId);
        return ResponseEntity.ok(toDto(savedHotel));
    }

    // 기존 경로
    @GetMapping("/my")
    public ResponseEntity<List<HotelDto>> getMyHotels(@RequestHeader("Authorization") String authHeader) {
        log.info("1. [OwnerHotelController] /api/owner/hotels/my 호출");
        try {
            Long ownerId = getUserIdFromTokenLenient(authHeader);
            List<Hotel> hotels = hotelService.getHotelsByOwner(ownerId);
            List<HotelDto> hotelDtos = hotels.stream().map(HotelDto::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(hotelDtos);
        } catch (Exception e) {
            log.error("[OwnerHotelController] 내 호텔 조회 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 프론트가 /my-hotels 를 호출하는 로그가 있어 alias 추가
    @GetMapping("/my-hotels")
    public ResponseEntity<List<HotelDto>> getMyHotelsAlias(@RequestHeader("Authorization") String authHeader) {
        return getMyHotels(authHeader);
    }

    // 오너의 모든 예약 (로그에 /owner/{id}/reservations 호출 흔적)
    @GetMapping("/owner/{ownerId}/reservations")
    public ResponseEntity<List<ReservationDtos.OwnerReservationResponse>> getReservationsOfOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(hotelService.getReservationsByOwner(ownerId));
    }

    // 숫자만 매칭
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<HotelDto> getHotel(@PathVariable Long id) {
        Hotel hotel = hotelService.getHotel(id);
        return ResponseEntity.ok(HotelDto.fromEntity(hotel));
    }

    // 숫자만 매칭
    @PostMapping("/{id:\\d+}")
    public ResponseEntity<HotelDto> updateHotel(
            @PathVariable Long id,
            @RequestPart("hotel") HotelDto hotelDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestHeader("Authorization") String authHeader) {

        log.info("1. [OwnerHotelController-수정] /api/owner/hotels/{} 호출", id);
        Long ownerId = getUserIdFromTokenLenient(authHeader);

        Hotel existingHotel = hotelService.getHotel(id);
        if (!existingHotel.getOwner().getId().equals(ownerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<String> imageUrls = new ArrayList<>();
        if (hotelDto.getImageUrls() != null) imageUrls.addAll(hotelDto.getImageUrls());
        if (files != null && !files.isEmpty()) {
            imageUrls.addAll(files.stream().map(fileStorageService::store).toList());
        }

        HotelDto updatedHotelDto = hotelService.updateHotel(id, hotelDto, imageUrls, hotelDto.getAmenityIds());
        log.info("5. [OwnerHotelController-수정] 완료, 호텔 ID: {}", updatedHotelDto.getId());
        return ResponseEntity.ok(updatedHotelDto);
    }

    // 숫자만 매칭
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        log.info("1. [OwnerHotelController-삭제] /api/owner/hotels/{} 호출", id);
        try {
            Long ownerId = getUserIdFromTokenLenient(authHeader);
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

    @GetMapping("/{hotelId:\\d+}/rooms")
    public ResponseEntity<List<RoomDto>> getRoomsByHotel(@PathVariable Long hotelId) {
        List<RoomDto> roomDtos = roomService.findByHotelId(hotelId);
        return ResponseEntity.ok(roomDtos);
    }

    @PostMapping("/{hotelId:\\d+}/rooms")
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

    @PutMapping("/rooms/{roomId:\\d+}")
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

    @DeleteMapping("/rooms/{roomId:\\d+}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        roomService.deleteRoom(roomId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard/sales-summary")
    public ResponseEntity<DashboardDto> getSalesSummary(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromTokenLenient(authHeader);
        DashboardDto summary = hotelService.getSalesSummary(ownerId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/dashboard/daily-sales")
    public ResponseEntity<List<DailySalesDto>> getDailySales(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SalesChartRequestDto requestDto) {
        Long ownerId = getUserIdFromTokenLenient(authHeader);
        List<DailySalesDto> dailySales = hotelService.getDailySales(ownerId, requestDto);
        return ResponseEntity.ok(dailySales);
    }

    @GetMapping("/dashboard/activity")
    public ResponseEntity<ReservationDtos.DashboardActivityResponse> getDashboardActivity(@RequestHeader("Authorization") String authHeader) {
        Long ownerId = getUserIdFromTokenLenient(authHeader);
        return ResponseEntity.ok(hotelService.getDashboardActivity(ownerId));
    }

    // ===== util =====
    private Long getUserIdFromTokenLenient(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효하지 않은 인증 헤더입니다.");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.extractAllClaims(token);
        Object v = claims.get("userId");
        if (v == null) v = claims.get("id");
        if (v == null) v = claims.get("user_id");
        if (v == null) v = claims.get("uid");
        if (v == null) {
            Object sub = claims.get("sub");
            if (sub != null && sub.toString().matches("\\d+")) v = sub;
        }
        if (v == null) throw new IllegalArgumentException("토큰에 userId/id/uid/sub(숫자) 없음");
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Long l) return l;
        return Long.parseLong(v.toString());
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
