package com.example.backend.fe_hotel_detail.controller;

import com.example.backend.fe_hotel_detail.dto.HotelDetailDto;
import com.example.backend.fe_hotel_detail.service.FeHotelDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("feHotelController") // ★ 빈 이름 고정
@RequestMapping("/api")              // FE는 기존 경로 유지 (/api/hotels/{id})
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class HotelController {
    private final FeHotelDetailService hotelService;

    @GetMapping("/hotels/{id}")
    public ResponseEntity<HotelDetailDto> getHotel(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelDetail(id));
    }
}
