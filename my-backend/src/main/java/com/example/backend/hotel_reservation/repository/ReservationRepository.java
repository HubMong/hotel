package com.example.backend.hotel_reservation.repository;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findTop500ByStatusAndExpiresAtBefore(ReservationStatus status, Instant cutoff);

    // 마이페이지(사용자별)
    @Query("""
           SELECT r
           FROM Reservation r
           WHERE r.userId = :userId
             AND r.status <> com.example.backend.hotel_reservation.domain.ReservationStatus.EXPIRED
           ORDER BY r.createdAt DESC
           """)
    Page<Reservation> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 오너: 자신의 호텔들의 모든 예약
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.roomId IN (
               SELECT ro.id FROM Room ro
               WHERE ro.hotel.owner.id = :ownerId
           )
           ORDER BY r.createdAt DESC
           """)
    List<Reservation> findAllByHotelOwnerId(@Param("ownerId") Long ownerId);

    // 오너: 오늘 체크인
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.roomId IN (
               SELECT ro.id FROM Room ro
               WHERE ro.hotel.owner.id = :ownerId
           )
             AND r.startDate >= :startOfDay
             AND r.startDate < :endOfDay
             AND r.status = com.example.backend.hotel_reservation.domain.ReservationStatus.COMPLETED
           """)
    List<Reservation> findCheckInsForOwnerByDateRange(@Param("ownerId") Long ownerId,
                                                      @Param("startOfDay") Instant startOfDay,
                                                      @Param("endOfDay") Instant endOfDay);

    // 오너: 오늘 체크아웃
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.roomId IN (
               SELECT ro.id FROM Room ro
               WHERE ro.hotel.owner.id = :ownerId
           )
             AND r.endDate >= :startOfDay
             AND r.endDate < :endOfDay
             AND r.status = com.example.backend.hotel_reservation.domain.ReservationStatus.COMPLETED
           """)
    List<Reservation> findCheckOutsForOwnerByDateRange(@Param("ownerId") Long ownerId,
                                                       @Param("startOfDay") Instant startOfDay,
                                                       @Param("endOfDay") Instant endOfDay);

    // 오너: 최근 예약 (createdAt 기준) — Instant로 통일
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.roomId IN (
               SELECT ro.id FROM Room ro
               WHERE ro.hotel.owner.id = :ownerId
           )
             AND r.status = com.example.backend.hotel_reservation.domain.ReservationStatus.COMPLETED
             AND r.createdAt >= :startDate
           ORDER BY r.createdAt DESC
           """)
    List<Reservation> findRecentReservationsForOwner(@Param("ownerId") Long ownerId,
                                                     @Param("startDate") Instant startDate,
                                                     Pageable pageable);

    // 오너 권한 확인: 해당 예약이 이 오너 소유 호텔의 예약인지
    @Query("""
           SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
           FROM Reservation r
           WHERE r.id = :reservationId
             AND r.roomId IN (
               SELECT rm.id FROM Room rm
               WHERE rm.hotel.owner.id = :ownerId
             )
           """)
    boolean isOwnedBy(@Param("reservationId") Long reservationId,
                      @Param("ownerId") Long ownerId);
}
