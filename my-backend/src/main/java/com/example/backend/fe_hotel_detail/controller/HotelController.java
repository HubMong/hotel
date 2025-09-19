// src/main/java/com/example/backend/fe_hotel_detail/controller/HotelController.java
package com.example.backend.fe_hotel_detail.controller;

import com.example.backend.fe_hotel_detail.dto.HotelDetailDto;
import com.example.backend.fe_hotel_detail.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class HotelController {
    private final HotelService hotelService;

    @GetMapping("/hotels/{id}")
    public ResponseEntity<HotelDetailDto> getHotel(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelDetail(id));
    }
}
