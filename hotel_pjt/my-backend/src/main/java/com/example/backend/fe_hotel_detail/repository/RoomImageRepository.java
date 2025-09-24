package com.example.backend.fe_hotel_detail.repository;

import com.example.backend.fe_hotel_detail.domain.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoomIdInOrderBySortNoAsc(List<Long> roomIds);
}
