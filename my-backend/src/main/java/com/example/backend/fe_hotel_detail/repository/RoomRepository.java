package com.example.backend.fe_hotel_detail.repository;

import com.example.backend.fe_hotel_detail.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelId(Long hotelId);
}
