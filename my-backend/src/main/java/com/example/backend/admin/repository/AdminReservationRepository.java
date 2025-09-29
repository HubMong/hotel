// src/main/java/com/example/backend/admin/repository/AdminReservationRepository.java
package com.example.backend.admin.repository;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository("adminReservationRepository") // (선택) 명시적 빈 이름
public interface AdminReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
       SELECT r FROM Reservation r
       WHERE (:status IS NULL OR r.status = :status)
         AND (:from IS NULL OR r.createdAt >= :from)
         AND (:to   IS NULL OR r.createdAt <= :to)
         AND (:stayFrom IS NULL OR r.endDate >= :stayFrom)
         AND (:stayTo   IS NULL OR r.startDate <= :stayTo)
    """)
    Page<Reservation> search(@Param("status") ReservationStatus status,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             @Param("stayFrom") Instant stayFrom,
                             @Param("stayTo") Instant stayTo,
                             Pageable pageable);

    long countByCreatedAtBetween(Instant from, Instant to);

    @Query(value = """
        SELECT 
            r.id AS reservationId,
            r.transaction_id AS transactionId,
            r.num_adult AS numAdult,
            r.num_kid AS numKid,
            r.start_date AS startDate,
            r.end_date AS endDate,
            r.created_at AS createdAt,
            r.status AS reservationStatus,
            r.expires_at AS expiresAt,

            h.id AS hotelId,
            COALESCE(h.name, '호텔 정보 없음') AS hotelName,
            COALESCE(h.address, '주소 정보 없음') AS hotelAddress,
            COALESCE(h.star_rating, 0) AS starRating,

            rm.id AS roomId,
            COALESCE(rm.name, '객실 정보 없음') AS roomName,
            COALESCE(rm.room_size, '객실 타입 정보 없음') AS roomType,
            COALESCE(rm.capacity_min, 0) AS capacityMin,
            COALESCE(rm.capacity_max, 0) AS capacityMax,
            COALESCE(rm.price, 0) AS roomPrice,

            u.id AS userId,
            COALESCE(u.name, '사용자 정보 없음') AS userName,
            COALESCE(u.email, '이메일 정보 없음') AS userEmail,
            COALESCE(u.phone, '전화번호 정보 없음') AS userPhone,

            p.id AS paymentId,
            COALESCE(p.payment_method, '결제 정보 없음') AS paymentMethod,
            COALESCE(p.base_price, 0) AS basePrice,
            COALESCE(p.total_price, 0) AS totalPrice,
            COALESCE(p.tax, 0) AS tax,
            COALESCE(p.discount, 0) AS discount,
            COALESCE(p.status, '결제 정보 없음') AS paymentStatus,
            p.created_at AS paymentCreatedAt,
            p.receipt_url AS receiptUrl
        FROM reservation r
        LEFT JOIN room rm ON r.room_id = rm.id
        LEFT JOIN hotel h ON rm.hotel_id = h.id
        LEFT JOIN app_user u ON r.user_id = u.id
        LEFT JOIN payment p ON r.id = p.reservation_id
        WHERE (:status IS NULL OR r.status = :status)
          AND (:from IS NULL OR r.created_at >= :from) 
          AND (:to IS NULL OR r.created_at <= :to)
          AND (:stayFrom IS NULL OR r.end_date >= :stayFrom) 
          AND (:stayTo IS NULL OR r.start_date <= :stayTo)
          AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
          AND (:userName IS NULL OR u.name LIKE CONCAT('%', :userName, '%'))
          AND (:paymentStatus IS NULL OR p.status = :paymentStatus)
        ORDER BY r.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM reservation r
        LEFT JOIN room rm ON r.room_id = rm.id
        LEFT JOIN hotel h ON rm.hotel_id = h.id
        LEFT JOIN app_user u ON r.user_id = u.id
        LEFT JOIN payment p ON r.id = p.reservation_id
        WHERE (:status IS NULL OR r.status = :status)
          AND (:from IS NULL OR r.created_at >= :from) 
          AND (:to IS NULL OR r.created_at <= :to)
          AND (:stayFrom IS NULL OR r.end_date >= :stayFrom) 
          AND (:stayTo IS NULL OR r.start_date <= :stayTo)
          AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
          AND (:userName IS NULL OR u.name LIKE CONCAT('%', :userName, '%'))
          AND (:paymentStatus IS NULL OR p.status = :paymentStatus)
        """,
        nativeQuery = true)
    Page<Object[]> searchWithDetails(@Param("status") String status,
                                     @Param("from") java.time.LocalDateTime from,
                                     @Param("to") java.time.LocalDateTime to,
                                     @Param("stayFrom") java.time.LocalDateTime stayFrom,
                                     @Param("stayTo") java.time.LocalDateTime stayTo,
                                     @Param("hotelName") String hotelName,
                                     @Param("userName") String userName,
                                     @Param("paymentStatus") String paymentStatus,
                                     Pageable pageable);

    @Query(value = """
        SELECT 
            r.id AS reservationId,
            r.transaction_id AS transactionId,
            r.num_adult AS numAdult,
            r.num_kid AS numKid,
            r.start_date AS startDate,
            r.end_date AS EndDate,
            r.created_at AS createdAt,
            r.status AS reservationStatus,
            r.expires_at AS expiresAt,

            h.id AS hotelId,
            COALESCE(h.name, '호텔 정보 없음') AS hotelName,
            COALESCE(h.address, '주소 정보 없음') AS hotelAddress,
            COALESCE(h.star_rating, 0) AS starRating,

            rm.id AS roomId,
            COALESCE(rm.name, '객실 정보 없음') AS roomName,
            COALESCE(rm.room_size, '객실 타입 정보 없음') AS roomType,
            COALESCE(rm.capacity_min, 0) AS capacityMin,
            COALESCE(rm.capacity_max, 0) AS capacityMax,
            COALESCE(rm.price, 0) AS roomPrice,

            u.id AS userId,
            COALESCE(u.name, '사용자 정보 없음') AS userName,
            COALESCE(u.email, '이메일 정보 없음') AS userEmail,
            COALESCE(u.phone, '전화번호 정보 없음') AS userPhone,

            p.id AS paymentId,
            COALESCE(p.payment_method, '결제 정보 없음') AS paymentMethod,
            COALESCE(p.base_price, 0) AS basePrice,
            COALESCE(p.total_price, 0) AS totalPrice,
            COALESCE(p.tax, 0) AS tax,
            COALESCE(p.discount, 0) AS discount,
            COALESCE(p.status, '결제 정보 없음') AS paymentStatus,
            p.created_at AS paymentCreatedAt,
            p.receipt_url AS receiptUrl
        FROM reservation r
        LEFT JOIN room rm ON r.room_id = rm.id
        LEFT JOIN hotel h ON rm.hotel_id = h.id
        LEFT JOIN app_user u ON r.user_id = u.id
        LEFT JOIN payment p ON r.id = p.reservation_id
        WHERE r.id = :reservationId
        """, nativeQuery = true)
    Object[] findDetailById(@Param("reservationId") Long reservationId);
}
