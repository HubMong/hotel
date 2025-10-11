package com.example.backend.hotel_search.service;

import com.example.backend.hotel_search.dto.HotelProjectionOnly;
import com.example.backend.hotel_search.repository.HotelSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
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
        String keyword = (q == null || q.isBlank()) ? null : q.trim();

        try {
            // 정교 쿼리 (room_price_policy 등 있으면 사용)
            return repo.search(keyword, checkIn, checkOut, minPrice, maxPrice, adults, children, pageable);
        } catch (DataAccessException ex) {
            // 테이블/컬럼 부족 등으로 실패 → 심플 쿼리 폴백
            return repo.searchSimple(keyword, minPrice, maxPrice, pageable);
        }
    }
}
