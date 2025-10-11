package com.example.backend.hotel_reservation.repository;

import com.example.backend.hotel_reservation.domain.RoomInventory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomInventoryRepository extends JpaRepository<RoomInventory, Long> {
    // 기본 조회
    List<RoomInventory> findByRoomId(Long roomId);
    Optional<RoomInventory> findByRoomIdAndDate(Long roomId, LocalDate date);

    // 비관적 락 (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ri from RoomInventory ri where ri.roomId = :roomId and ri.date = :date")
    Optional<RoomInventory> findWithLock(@Param("roomId") Long roomId, @Param("date") LocalDate date);
}
