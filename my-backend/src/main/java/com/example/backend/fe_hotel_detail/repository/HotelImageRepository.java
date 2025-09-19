package com.example.backend.fe_hotel_detail.repository;

import com.example.backend.fe_hotel_detail.domain.HotelImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelImageRepository extends JpaRepository<HotelImage, Long> {
    List<HotelImage> findByHotelIdOrderBySortNoAsc(Long hotelId);
}
