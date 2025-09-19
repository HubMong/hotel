package com.example.backend.fe_hotel_detail.repository;

import com.example.backend.fe_hotel_detail.domain.Hotel; // ← 이거여야 함
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {}
