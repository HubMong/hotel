// src/main/java/com/example/backend/hotel_reservation/controller/ReservationController.java
package com.example.backend.hotel_reservation.controller;

import com.example.backend.hotel_reservation.dto.ReservationDtos;
import com.example.backend.hotel_reservation.dto.ReservationDtos.*;
import com.example.backend.hotel_reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.backend.authlogin.config.JwtUtil;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;
    private final JwtUtil jwtUtil;

    @PostMapping("/hold")
    public HoldResponse hold(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody HoldRequest req
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }
        final String token = authHeader.substring(7);
        Long userId = jwtUtil.extractClaim(token, claims -> {
            Object v = claims.get("userId");
            if (v == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No userId in token");
            if (v instanceof Number n) return n.longValue();
            try { return Long.parseLong(v.toString()); }
            catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid userId in token"); }
        });

        req.setUserId(userId);
        return service.hold(req);
    }

    @PostMapping("/{id}/confirm")
    public void confirm(@PathVariable Long id) { service.confirm(id); }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id) { service.cancel(id); }

    @PostMapping("/{id}/expire")
    public void expire(@PathVariable Long id) { service.expire(id); }

    @GetMapping("/{id}")
    public ReservationDetail get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/user/{userId}")
    public List<ReservationDtos.ReservationSummary> getByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getByUserId(userId, page, size);
    }

    @GetMapping("/my")
    public List<ReservationDtos.ReservationSummary> getMy(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Bearer token");
        }
        final String token = authHeader.substring(7);
        Long userId = jwtUtil.extractClaim(token, claims -> {
            Object v = claims.get("userId");
            if (v == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No userId in token");
            if (v instanceof Number n) return n.longValue();
            try { return Long.parseLong(v.toString()); }
            catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid userId in token"); }
        });

        return service.getByUserId(userId, page, size);
    }

    // 🔧 여기부터 수정
    @GetMapping("/findoverlap")
    public Map<Long, Long> findOverlap(
            @RequestParam String checkIn,               // 문자열로 받고
            @RequestParam String checkOut,              // 아래에서 유연 파싱
            @RequestParam(value = "roomIds",  required = false) List<Long> roomIds,
            @RequestParam(value = "roomIds[]",required = false) List<Long> roomIdsBracket
    ) {
        // roomIds / roomIds[] 둘 중 뭐가 와도 합치기
        List<Long> ids = new ArrayList<>();
        if (roomIds != null) ids.addAll(roomIds);
        if (roomIdsBracket != null) ids.addAll(roomIdsBracket);
        if (ids.isEmpty()) return Collections.emptyMap();

        // YYYY-MM-DD 또는 ISO-8601(Instant) 모두 허용
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Instant ci = parseFlexibleInstant(checkIn,  zone, true);   // start-of-day(포함)
        Instant co = parseFlexibleInstant(checkOut, zone, true);   // start-of-day(배타) – 반열림 구간 [ci,co)

        if (!co.isAfter(ci)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "checkOut must be after checkIn");
        }

        return service.findOverlapLong(ids, ci, co);
    }

    private Instant parseFlexibleInstant(String raw, ZoneId zone, boolean atStartOfDay) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date required");
        }
        try {
            // '2025-10-09' 같은 YMD
            if (raw.length() == 10 && raw.charAt(4) == '-' && raw.charAt(7) == '-') {
                LocalDate d = LocalDate.parse(raw);
                return (atStartOfDay ? d.atStartOfDay(zone) : d.atTime(23, 59, 59).atZone(zone)).toInstant();
            }
            // 그 외는 Instant로 시도
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid date: " + raw);
        }
    }
}
