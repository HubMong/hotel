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

    private static String norm(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    @GetMapping("/hotels")
    public Page<HotelProjectionOnly> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String destination,
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
        String keyword = norm(q);
        if (keyword == null) keyword = norm(destination);

        return service.search(
            keyword,
            norm(checkIn),
            norm(checkOut),
            rooms,
            adults,
            children,
            minPrice,
            maxPrice,
            page,
            size
        );
    }
}
