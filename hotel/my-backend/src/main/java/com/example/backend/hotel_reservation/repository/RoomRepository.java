package com.example.backend.hotel_reservation.repository;

import com.example.backend.hotel_reservation.domain.Room;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // ✅ 호텔 상세에서 객실 리스트 필요
    List<Room> findByHotelId(Long hotelId);

    // (기존 사용하던 쿼리 유지)
    @Query("select r.hotelId from Room r where r.id = :roomId")
    Long findHotelIdByRoomId(@Param("roomId") Long roomId);
}
