// src/main/java/com/example/backend/HotelOwner/repository/RoomRepository.java
package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByRoomType(Room.RoomType roomType);

    /** 특정 호텔의 객실 전체 */
    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.hotel WHERE r.hotel.id = :hotelId")
    List<Room> findByHotelId(@Param("hotelId") Long hotelId);

    List<Room> findByHotel(Hotel hotel);

    // ⚠️ 잘못된 파싱을 막기 위해 올바른 경로 메서드 추가
    @Deprecated // 사용 지양: Spring Data가 'HotelOwnerEmail'을 하나의 프로퍼티로 오인할 수 있음
    boolean existsByIdAndHotelOwnerEmail(Long roomId, String ownerEmail);

    /** ✅ 올바른 프로퍼티 경로 버전 */
    boolean existsByIdAndHotel_Owner_Email(Long roomId, String ownerEmail);

    /** 이미지 포함 로딩 */
    @Query("SELECT DISTINCT r FROM Room r LEFT JOIN FETCH r.images WHERE r.hotel.id = :hotelId")
    List<Room> findByHotelIdWithImages(@Param("hotelId") Long hotelId);

    @Query("SELECT r.hotel.id FROM Room r WHERE r.id = :roomId")
    Long findHotelIdByRoomId(@Param("roomId") Long roomId);

    // Admin용 페이징 (선택)
    Page<Room> findByHotel_IdAndNameContaining(Long hotelId, String name, Pageable pageable);
    Page<Room> findByHotel_Id(Long hotelId, Pageable pageable);
    Page<Room> findByNameContaining(String name, Pageable pageable);

    /** 호텔에 객실이 한 개라도 있는지 */
    boolean existsByHotel_Id(Long hotelId);
}
