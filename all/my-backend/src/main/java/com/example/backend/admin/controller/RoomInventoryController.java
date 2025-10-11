package com.example.backend.admin.controller;

import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.inventory.RoomInventoryOverride;
import com.example.backend.admin.inventory.RoomInventoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/admin/inventory", "/api/admin/rooms"})
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class RoomInventoryController {
    private final RoomInventoryService service;

    @GetMapping("/overrides")
    public ResponseEntity<ApiResponse<List<RoomInventoryOverride>>> list(
            @RequestParam List<Long> roomIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var list = service.listForRoomsAndMonth(roomIds, from, to);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/{roomId}/overrides")
    public ResponseEntity<ApiResponse<List<RoomInventoryOverride>>> create(
            @PathVariable Long roomId,
            @RequestBody CreateOverrideRequest req
    ) {
        LocalDate from = req.getFrom();
        LocalDate to = req.getTo() != null ? req.getTo() : req.getFrom();
        var list = service.createOverrides(roomId, from, to, req.getStatus(), req.getNote());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // Update or create overrides for a date range (idempotent upsert)
    @PutMapping("/{roomId}/overrides")
    public ResponseEntity<ApiResponse<List<RoomInventoryOverride>>> upsert(
            @PathVariable Long roomId,
            @RequestBody CreateOverrideRequest req
    ) {
        LocalDate from = req.getFrom();
        LocalDate to = req.getTo() != null ? req.getTo() : req.getFrom();
        var list = service.upsertOverrides(roomId, from, to, req.getStatus(), req.getNote());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    // Update or create a single-day override by date path variable
    @PutMapping("/{roomId}/overrides/{date}")
    public ResponseEntity<ApiResponse<RoomInventoryOverride>> upsertOne(
            @PathVariable Long roomId,
            @PathVariable @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody CreateOverrideRequest req
    ) {
        var ov = service.upsertOverride(roomId, date, req.getStatus(), req.getNote());
        return ResponseEntity.ok(ApiResponse.ok(ov));
    }

    @DeleteMapping("/overrides/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.deleteOverride(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Data
    public static class CreateOverrideRequest {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate from;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate to;
        private String status; // open|closed|maintenance|cleaning
        private String note;
    }
}
