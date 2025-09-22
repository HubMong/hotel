package com.example.backend.hotel_search.service;

import com.example.backend.hotel_search.dto.HotelProjectionOnly;
import com.example.backend.hotel_search.repository.HotelSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HotelSearchService {
    private final HotelSearchRepository repo;

    public Page<HotelProjectionOnly> search(
            String q,
            String checkIn,
            String checkOut,
            Integer rooms,      // 현재 미사용
            Integer adults,
            Integer children,
            Integer minPrice,
            Integer maxPrice,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return repo.search(q, checkIn, checkOut, minPrice, maxPrice, adults, children, pageable);
    }
}
