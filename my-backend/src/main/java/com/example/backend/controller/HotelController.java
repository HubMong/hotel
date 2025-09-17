package com.example.backend.controller;

import com.example.backend.dto.HotelDetailDto;
import com.example.backend.service.HotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping("/ping")
    public String ping(){ return "pong"; }

    @GetMapping("/hotels/{id}")
    public ResponseEntity<HotelDetailDto> getHotel(@PathVariable Long id){
        return ResponseEntity.ok(hotelService.getHotelDetail(id));
    }
}
