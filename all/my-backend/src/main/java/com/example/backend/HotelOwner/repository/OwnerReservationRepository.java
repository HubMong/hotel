// src/main/java/com/example/backend/HotelOwner/repository/OwnerReservationRepository.java
package com.example.backend.HotelOwner.repository;

import com.example.backend.hotel_reservation.domain.Reservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerReservationRepository extends JpaRepository<Reservation, Long> {

    /* ─────────────────────────────────────────────────────────
       ✅ 기존: 호텔 단건 기준 조회(여전히 다른 곳에서 쓰면 유지)
       ───────────────────────────────────────────────────────── */
    @Query(value = """
            SELECT res.*
            FROM reservation res
            WHERE res.room_id IN (SELECT r.id FROM room r WHERE r.hotel_id = :hotelId)
            """, nativeQuery = true)
    List<Reservation> findAllByHotelId(@Param("hotelId") Long hotelId);

    @Query(value = "SELECT count(*) FROM reservation res " +
            "JOIN room r ON res.room_id = r.id " +
            "WHERE res.start_date BETWEEN :start AND :end AND res.status = :status AND r.hotel_id = :hotelId",
            nativeQuery = true)
    long countByStartDateBetweenAndStatusAndRoomHotelId(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("status") String status,
            @Param("hotelId") Long hotelId);

    @Query(value = "SELECT count(*) FROM reservation res " +
            "JOIN room r ON res.room_id = r.id " +
            "WHERE res.end_date BETWEEN :start AND :end AND res.status = :status AND r.hotel_id = :hotelId",
            nativeQuery = true)
    long countByEndDateBetweenAndStatusAndRoomHotelId(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("status") String status,
            @Param("hotelId") Long hotelId);

    /* ✅ boolean 대신 long 카운트 (남겨둠) */
    @Query(value = """
            SELECT COUNT(*)
            FROM reservation res
            JOIN room r ON res.room_id = r.id
            WHERE r.hotel_id = :hotelId
            """, nativeQuery = true)
    long countAnyByHotelId(@Param("hotelId") Long hotelId);


    /* ─────────────────────────────────────────────────────────
       ✅ 추가: 오너(=user_id) 기준 전체 예약 조회 (여러 호텔 소유 지원)
       ───────────────────────────────────────────────────────── */
    @Query(value = """
        SELECT res.*
        FROM reservation res
        JOIN room r   ON res.room_id = r.id
        JOIN hotel h  ON r.hotel_id  = h.id
        WHERE h.user_id = :ownerId
        ORDER BY res.start_date ASC, res.id ASC
        """, nativeQuery = true)
    List<Reservation> findAllByOwnerId(@Param("ownerId") Long ownerId);

    /* ✅ 추가: 오너 소유 검증 포함한 단건 조회 (권한 체크를 DB에서 해결) */
    @Query(value = """
        SELECT res.*
        FROM reservation res
        JOIN room r   ON res.room_id = r.id
        JOIN hotel h  ON r.hotel_id  = h.id
        WHERE h.user_id = :ownerId
          AND res.id    = :reservationId
        """, nativeQuery = true)
    Optional<Reservation> findOneForOwner(@Param("ownerId") Long ownerId,
                                          @Param("reservationId") Long reservationId);
}
