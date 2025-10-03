package com.example.backend.admin.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RoomInventoryOverrideRepository extends JpaRepository<RoomInventoryOverride, Long> {
    List<RoomInventoryOverride> findByRoomIdAndDateBetween(Long roomId, LocalDate from, LocalDate to);
    List<RoomInventoryOverride> findByRoomIdInAndDateBetween(List<Long> roomIds, LocalDate from, LocalDate to);
    java.util.Optional<RoomInventoryOverride> findByRoomIdAndDate(Long roomId, LocalDate date);
}
