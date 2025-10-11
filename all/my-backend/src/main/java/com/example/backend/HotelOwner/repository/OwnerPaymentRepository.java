package com.example.backend.HotelOwner.repository;

import com.example.backend.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OwnerPaymentRepository extends JpaRepository<Payment, Long> {

        Optional<Payment> findByReservationId(Long reservationId);

        @Query(value = "SELECT COALESCE(SUM(p.total_price), 0) FROM payment p " +
                        "JOIN reservation res ON p.reservation_id = res.id " +
                        "JOIN room r ON res.room_id = r.id " +
                        "WHERE p.created_at >= :start AND p.status = 'COMPLETED' AND r.hotel_id = :hotelId",
                nativeQuery = true)
        Long getSalesFromByHotelId(@Param("start") LocalDateTime start, @Param("hotelId") Long hotelId);

        @Query(value = "SELECT DATE_FORMAT(p.created_at, :format) as period, SUM(p.total_price) as sales " +
                "FROM payment p " +
                "JOIN reservation r ON p.reservation_id = r.id " +
                "JOIN room rm ON r.room_id = rm.id " +
                "WHERE p.status = 'COMPLETED' AND rm.hotel_id = :hotelId AND p.created_at BETWEEN :start AND :end " + // 수정된 부분: rm.hotel_id
                "AND (:roomType IS NULL OR rm.room_type = :roomType) " +
                "GROUP BY period ORDER BY period ASC", nativeQuery = true)
        List<Object[]> findSalesByPeriodAndHotelId(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                                @Param("roomType") String roomType, @Param("format") String format,
                                                @Param("hotelId") Long hotelId);
}