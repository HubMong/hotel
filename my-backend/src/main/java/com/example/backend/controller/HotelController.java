package com.example.backend.controller;

import com.example.backend.domain.Hotel;
import com.example.backend.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@CrossOrigin(origins = "http://localhost:5174") // Vue.js 개발 서버 포트 허용
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @GetMapping
    public List<Hotel> getHotels(
        @RequestParam(required = false) String destination,
        @RequestParam(required = false) Integer guests
    ) {
        return hotelService.searchHotels(destination, guests);
    }
}