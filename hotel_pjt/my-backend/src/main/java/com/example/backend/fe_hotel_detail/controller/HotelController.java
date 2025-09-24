// src/main/java/com/example/backend/fe_hotel_detail/controller/HotelController.java
package com.example.backend.fe_hotel_detail.controller;

import com.example.backend.fe_hotel_detail.dto.HotelDetailDto;
import com.example.backend.fe_hotel_detail.dto.RoomSummaryDto;
import com.example.backend.fe_hotel_detail.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.backend.fe_hotel_detail.service.RoomService; // ✅ RoomService import

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class HotelController {
    private final HotelService hotelService;
        private final RoomService roomService; // ✅ RoomService 주입


    @GetMapping("/hotels/{id}")
    public ResponseEntity<HotelDetailDto> getHotel(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getHotelDetail(id));
    }
    @GetMapping("/rooms/{id}/summary") 
    public ResponseEntity<RoomSummaryDto> getRoomSummary(@PathVariable Long id) {
        // RoomService를 통해 DTO를 받아옵니다. (서비스와 DTO는 새로 만들어야 합니다.)
        RoomSummaryDto summary = roomService.getRoomSummary(id); 
        return ResponseEntity.ok(summary);
    }
}
