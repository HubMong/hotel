// src/main/java/com/example/backend/HotelOwner/repository/HotelImageRepository.java
package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {

    // 둘 다 동작합니다. 프로젝트 내 호출부 상황에 맞춰 아무거나 사용하세요.
    List<HotelImage> findByHotelIdOrderBySortNoAsc(Long hotelId);      // 언더스코어 없음
    List<HotelImage> findByHotel_IdOrderBySortNoAsc(Long hotelId);     // 언더스코어 있음 (기존)

    void deleteByHotel_Id(Long hotelId);

    boolean existsByIdAndHotel_Id(Long imageId, Long hotelId);
    Optional<HotelImage> findByIdAndHotel_Id(Long imageId, Long hotelId);
}
