// src/main/java/com/example/backend/hotel_support/repository/HotelInquiryRepository.java

package com.example.backend.hotel_support.repository;

import com.example.backend.hotel_support.domain.HotelInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HotelInquiryRepository extends JpaRepository<HotelInquiry, Long> {
    
    // 오너의 모든 호텔 문의 조회 (필터 없음)
    List<HotelInquiry> findByHotelIdInOrderByCreatedAtDesc(List<Long> hotelIds);

    // 오너의 호텔 문의 + 상태(Status) 필터링 조회
    List<HotelInquiry> findByHotelIdInAndStatusOrderByCreatedAtDesc(List<Long> hotelIds, String status);

    // ⭐ [추가] 사용자 본인의 문의 내역 조회용 쿼리
    List<HotelInquiry> findByUserIdOrderByCreatedAtDesc(Long userId);
}