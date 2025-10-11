package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OwnerRoomRepository extends JpaRepository<Room, Long> {
    // 특정 호텔에 속한 모든 객실을 찾는 기능
    List<Room> findAllByHotel(Hotel hotel);
}