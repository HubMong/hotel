package com.example.backend.fe_hotel_detail.repository;

import com.example.backend.HotelOwner.domain.RoomImage; // ★ FE 엔티티로 교체!
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeRoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoomIdInOrderBySortNoAsc(List<Long> roomIds);
    // 필요하면 단건도
    List<RoomImage> findByRoomIdOrderBySortNoAsc(Long roomId);
}
