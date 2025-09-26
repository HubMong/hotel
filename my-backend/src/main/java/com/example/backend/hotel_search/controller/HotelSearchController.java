package com.example.backend.hotel_search.controller;

import com.example.backend.hotel_search.dto.HotelProjectionOnly;
import com.example.backend.hotel_search.service.HotelSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
@RequiredArgsConstructor
public class HotelSearchController {

    private final HotelSearchService service;

    @GetMapping("/hotels")
    public Page<HotelProjectionOnly> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String destination, // ★ 프론트에서 destination으로 올 수 있음
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            @RequestParam(required = false) Integer rooms,
            @RequestParam(required = false) Integer adults,
            @RequestParam(required = false) Integer children,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String keyword = (q != null && !q.isBlank())
                ? q
                : (destination != null && !destination.isBlank() ? destination : null);
        return service.search(keyword, checkIn, checkOut, rooms, adults, children, minPrice, maxPrice, page, size);
    }
}
