package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.dto.OwnerReservationDto;
import com.example.backend.HotelOwner.service.OwnerReservationService;
import com.example.backend.authlogin.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/owner/reservations")
@RequiredArgsConstructor
public class OwnerReservationController {

    private final OwnerReservationService ownerReservationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<OwnerReservationDto.CalendarEvent>> getMyHotelReservations(@RequestHeader("Authorization") String auth) {
        Long ownerId = jwtUtil.extractUserId(auth.substring(7));
        return ResponseEntity.ok(ownerReservationService.getReservationsForOwner(ownerId));
    }
    
    @GetMapping("/{reservationId}")
    public ResponseEntity<OwnerReservationDto.DetailResponse> getReservationDetails(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long reservationId) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(auth.substring(7));
        return ResponseEntity.ok(ownerReservationService.getReservationDetails(ownerId, reservationId));
    }

    @PutMapping("/{reservationId}/check-in")
    public ResponseEntity<Void> checkIn(@RequestHeader("Authorization") String auth, @PathVariable Long reservationId) throws AccessDeniedException {
        ownerReservationService.checkIn(jwtUtil.extractUserId(auth.substring(7)), reservationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservationId}/check-out")
    public ResponseEntity<Void> checkOut(@RequestHeader("Authorization") String authorizationHeader, @PathVariable Long reservationId) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        ownerReservationService.checkOut(ownerId, reservationId);
        return ResponseEntity.ok().build();
    }

    //  체크인 취소 API 
    @PutMapping("/{reservationId}/cancel-checkin")
    public ResponseEntity<Void> cancelCheckIn(@RequestHeader("Authorization") String authorizationHeader, @PathVariable Long reservationId) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        // 서비스의 cancelCheckIn 메서드 호출
        ownerReservationService.cancelCheckIn(ownerId, reservationId);
        return ResponseEntity.ok().build();
    }

    // 체크아웃 취소 API 
    @PutMapping("/{reservationId}/cancel-checkout")
    public ResponseEntity<Void> cancelCheckOut(@RequestHeader("Authorization") String authorizationHeader, @PathVariable Long reservationId) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        // 서비스의 cancelCheckOut 메서드 호출
        ownerReservationService.cancelCheckOut(ownerId, reservationId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{reservationId}/cancel")
    public ResponseEntity<Void> cancelReservation(@RequestHeader("Authorization") String authorizationHeader, @PathVariable Long reservationId) throws AccessDeniedException {
        Long ownerId = jwtUtil.extractUserId(authorizationHeader.substring(7));
        ownerReservationService.cancelReservation(ownerId, reservationId);
        return ResponseEntity.ok().build();
    }
}   