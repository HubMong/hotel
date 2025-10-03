// src/main/java/com/example/backend/HotelOwner/repository/HotelImageRepository.java
package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
    List<HotelImage> findByHotelIdOrderBySortNoAsc(Long hotelId);
}