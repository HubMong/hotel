package com.example.backend.HotelOwner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.HotelOwner.domain.RoomImage;
import java.util.List;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoom_IdInOrderBySortNoAsc(List<Long> roomIds);
}