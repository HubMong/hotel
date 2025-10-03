package com.example.backend.HotelOwner.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 방 종류별 조회
    List<Room> findByRoomType(Room.RoomType roomType);

    // 특정 호텔의 모든 객실
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.hotel WHERE r.hotel.id = :hotelId")
    List<Room> findByHotelId(@Param("hotelId") Long hotelId);

    List<Room> findByHotel(Hotel hotel);

    boolean existsByIdAndHotelOwnerEmail(Long roomId, String ownerEmail);

    // ★ 이미지까지 한 번에 로딩
    @Query("SELECT DISTINCT r FROM Room r " +
           "LEFT JOIN FETCH r.images " +
           "WHERE r.hotel.id = :hotelId")
    List<Room> findByHotelIdWithImages(@Param("hotelId") Long hotelId);

    @Query("SELECT r.hotel.id FROM Room r WHERE r.id = :roomId")
    Long findHotelIdByRoomId(@Param("roomId") Long roomId);

    // Admin 페이징 조회 메서드
    org.springframework.data.domain.Page<Room> findByHotel_IdAndNameContaining(Long hotelId, String name, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Room> findByHotel_Id(Long hotelId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Room> findByNameContaining(String name, org.springframework.data.domain.Pageable pageable);
}
