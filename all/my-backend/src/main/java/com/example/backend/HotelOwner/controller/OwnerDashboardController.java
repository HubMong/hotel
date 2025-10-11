package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.dto.OwnerDashboardDto;
import com.example.backend.HotelOwner.dto.OwnerSalesGraphDataDto;
import com.example.backend.HotelOwner.service.OwnerDashboardService;
import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.authlogin.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;
    private final JwtUtil jwtUtil; 

    @GetMapping("/summary-cards/{hotelId}")
    public ResponseEntity<ApiResponse<OwnerDashboardDto>> getSummaryCards(
            @PathVariable Long hotelId,
            @RequestHeader("Authorization") String auth) {
        Long ownerId = jwtUtil.extractUserId(auth.substring(7));
        return ResponseEntity.ok(ApiResponse.ok(ownerDashboardService.getDashboardSummary(ownerId, hotelId)));
    }

    @GetMapping("/sales-graph/{hotelId}")
    public ResponseEntity<ApiResponse<OwnerSalesGraphDataDto>> getSalesGraph(
            @PathVariable Long hotelId,
            @RequestHeader("Authorization") String auth,
            @RequestParam(required = false) String roomType,
            @RequestParam(defaultValue = "daily") String analysisType,
            @RequestParam(defaultValue = "7d") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long ownerId = jwtUtil.extractUserId(auth.substring(7));
        return ResponseEntity.ok(ApiResponse.ok(ownerDashboardService.getSalesGraphData(ownerId, hotelId, roomType, period, analysisType, startDate, endDate)));
    }
}