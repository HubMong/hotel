package com.example.backend.payment.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.backend.payment.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 공통 결제 조회
    boolean existsByOrderId(String orderId);
    Payment findByOrderId(String orderId);
    Optional<Payment> findByReservationId(Long reservationId);
    Optional<Payment> findTopByReservationIdOrderByCreatedAtDesc(Long reservationId);
    List<Payment> findAllByReservationIdOrderByCreatedAtDesc(Long reservationId);
    
    // User를 포함한 결제 조회 (환불 처리용)
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.user WHERE p.id = :id")
    Optional<Payment> findByIdWithUser(@Param("id") Long id);

    // ===== Admin analytics/search methods migrated from admin repository =====

    @Query("SELECT p FROM Payment p WHERE (:status IS NULL OR p.status = :status) " +
      "AND (:from IS NULL OR p.createdAt >= :from) AND (:to IS NULL OR p.createdAt <= :to)")
    Page<Payment> search(@Param("status") Payment.Status status,
       @Param("from") LocalDateTime from,
       @Param("to") LocalDateTime to,
       Pageable pageable);

    @Query(value = """
		SELECT
			p.id AS paymentId,
			p.reservation_id AS reservationId,
			r.transaction_id AS transactionId,
			h.name AS hotelName,
			u.name AS userName,
			p.total_price AS totalPrice,
			p.payment_method AS paymentMethod,
			p.status AS paymentStatus,
			p.created_at AS createdAt,
			p.canceled_at AS canceledAt
		FROM payment p
		LEFT JOIN reservation r ON p.reservation_id = r.id
		LEFT JOIN room rm ON r.room_id = rm.id
		LEFT JOIN hotel h ON rm.hotel_id = h.id
		LEFT JOIN app_user u ON r.user_id = u.id
		WHERE (:status IS NULL OR p.status = :status)
		AND (:from IS NULL OR p.created_at >= :from)
		AND (:to IS NULL OR p.created_at <= :to)
		AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
		AND (:userName IS NULL OR u.name LIKE CONCAT('%', :userName, '%'))
		""",
      countQuery = """
		SELECT COUNT(*)
		FROM payment p
		LEFT JOIN reservation r ON p.reservation_id = r.id
		LEFT JOIN room rm ON r.room_id = rm.id
		LEFT JOIN hotel h ON rm.hotel_id = h.id
		LEFT JOIN app_user u ON r.user_id = u.id
		WHERE (:status IS NULL OR p.status = :status)
		AND (:from IS NULL OR p.created_at >= :from)
		AND (:to IS NULL OR p.created_at <= :to)
		AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
		AND (:userName IS NULL OR u.name LIKE CONCAT('%', :userName, '%'))
		""",
      nativeQuery = true)
    Page<Object[]> searchWithDetails(@Param("status") String status,
             @Param("from") LocalDateTime from,
             @Param("to") LocalDateTime to,
             @Param("hotelName") String hotelName,
             @Param("userName") String userName,
             Pageable pageable);

    @Query(value = "SELECT MONTH(created_at) AS m, COALESCE(SUM(total_price),0) AS revenue, COUNT(*) AS cnt " +
      "FROM payment WHERE (:year IS NULL OR YEAR(created_at)=:year) GROUP BY m ORDER BY m", nativeQuery = true)
    java.util.List<Object[]> sumMonthlyRevenue(@Param("year") Integer year);

    @Query(value = "SELECT h.id AS hotel_id, h.name AS hotel_name, COALESCE(SUM(p.total_price),0) AS revenue " +
      "FROM hotel h " +
      "JOIN room r ON r.hotel_id = h.id " +
      "JOIN reservation res ON res.room_id = r.id " +
      "JOIN payment p ON p.reservation_id = res.id AND p.status = 'COMPLETED' " +
      "WHERE (:year IS NULL OR YEAR(p.created_at) = :year) " +
      "GROUP BY h.id, h.name ORDER BY revenue DESC", nativeQuery = true)
    java.util.List<Object[]> hotelRevenueByYear(@Param("year") Integer year);

  @Query("SELECT COALESCE(SUM(p.totalPrice), 0) FROM Payment p WHERE p.createdAt BETWEEN :from AND :to AND p.status = :status")
  Long sumTotalPriceByCreatedAtBetween(@Param("from") LocalDateTime from,
                     @Param("to") LocalDateTime to,
                     @Param("status") Payment.Status status);

    @Query("SELECT p FROM Payment p WHERE p.reservationId = :reservationId")
    java.util.List<Payment> findAllByReservationId(@Param("reservationId") Long reservationId);

    @Query(value = "SELECT DATE(p.created_at) as d, COALESCE(SUM(p.total_price),0) as revenue " +
      "FROM payment p WHERE p.status='COMPLETED' AND p.created_at BETWEEN :from AND :to " +
      "GROUP BY d ORDER BY d", nativeQuery = true)
    java.util.List<Object[]> dailyRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT h.id, h.name, " +
      "COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.total_price ELSE 0 END),0) AS revenue, " +
      "COUNT(DISTINCT res.id) AS reservation_count, " +
      "COUNT(DISTINCT r.id) AS room_count, " +
      "COALESCE(AVG(rev.star_rating),0) AS avg_rating " +
      "FROM hotel h " +
      "LEFT JOIN room r ON r.hotel_id = h.id " +
      "LEFT JOIN reservation res ON res.room_id = r.id " +
      "LEFT JOIN payment p ON p.reservation_id = res.id " +
      "LEFT JOIN review rev ON rev.reservation_id = res.id " +
      "WHERE h.approval_status = 'APPROVED' " +
      "AND (:year IS NULL OR p.created_at IS NULL OR YEAR(p.created_at)=:year) " +
      "GROUP BY h.id, h.name " +
      "ORDER BY revenue DESC, h.created_at DESC", nativeQuery = true)
    java.util.List<Object[]> topHotelRevenue(@Param("year") Integer year);

    @Query(value = "SELECT h.id, h.name, 0 AS revenue, " +
      "COUNT(DISTINCT res.id) AS reservation_count, " +
      "COUNT(DISTINCT r.id) AS room_count, " +
      "0 AS avg_rating " +
      "FROM hotel h " +
      "LEFT JOIN room r ON r.hotel_id = h.id " +
      "LEFT JOIN reservation res ON res.room_id = r.id " +
      "WHERE h.approval_status = 'APPROVED' " +
      "GROUP BY h.id, h.name " +
      "ORDER BY h.created_at DESC", nativeQuery = true)
    java.util.List<Object[]> approvedHotelsWithZeroRevenue();

    @Query(value = "SELECT h.id AS hotel_id, h.name AS hotel_name, COALESCE(SUM(p.total_price),0) AS revenue, " +
      "COUNT(DISTINCT res.id) AS reservation_count " +
      "FROM hotel h " +
      "JOIN room r ON r.hotel_id = h.id " +
      "JOIN reservation res ON res.room_id = r.id " +
      "JOIN payment p ON p.reservation_id = res.id AND p.status='COMPLETED' " +
      "WHERE p.created_at BETWEEN :from AND :to " +
      "GROUP BY h.id, h.name ORDER BY revenue DESC", nativeQuery = true)
    java.util.List<Object[]> hotelRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = "SELECT DATE(p.created_at) AS period, COALESCE(SUM(p.total_price),0) AS amount, COUNT(*) AS cnt FROM payment p " +
      "LEFT JOIN reservation r ON p.reservation_id = r.id " +
      "LEFT JOIN room rm ON r.room_id = rm.id " +
      "WHERE p.status = 'COMPLETED' " +
      "AND p.created_at BETWEEN :from AND :to " +
      "AND (:hotelId IS NULL OR rm.hotel_id = :hotelId) " +
      "AND (:method IS NULL OR p.payment_method = :method) " +
      "GROUP BY period ORDER BY period", nativeQuery = true)
    java.util.List<Object[]> aggregateDaily(@Param("from") LocalDateTime from,
              @Param("to") LocalDateTime to,
              @Param("hotelId") Long hotelId,
              @Param("method") String method);

    @Query(value = "SELECT CONCAT(YEAR(p.created_at), '-', LPAD(WEEK(p.created_at, 1), 2, '0')) AS period, " +
      "COALESCE(SUM(p.total_price),0) AS amount, COUNT(*) AS cnt " +
      "FROM payment p " +
      "LEFT JOIN reservation r ON p.reservation_id = r.id " +
      "LEFT JOIN room rm ON r.room_id = rm.id " +
      "WHERE p.status = 'COMPLETED' " +
      "AND p.created_at BETWEEN :from AND :to " +
      "AND (:hotelId IS NULL OR rm.hotel_id = :hotelId) " +
      "AND (:method IS NULL OR p.payment_method = :method) " +
      "GROUP BY period ORDER BY period", nativeQuery = true)
    java.util.List<Object[]> aggregateWeekly(@Param("from") LocalDateTime from,
               @Param("to") LocalDateTime to,
               @Param("hotelId") Long hotelId,
               @Param("method") String method);

    @Query(value = "SELECT DATE_FORMAT(p.created_at, '%Y-%m') AS period, " +
      "COALESCE(SUM(p.total_price),0) AS amount, COUNT(*) AS cnt " +
      "FROM payment p " +
      "LEFT JOIN reservation r ON p.reservation_id = r.id " +
      "LEFT JOIN room rm ON r.room_id = rm.id " +
      "WHERE p.status = 'COMPLETED' " +
      "AND p.created_at BETWEEN :from AND :to " +
      "AND (:hotelId IS NULL OR rm.hotel_id = :hotelId) " +
      "AND (:method IS NULL OR p.payment_method = :method) " +
      "GROUP BY period ORDER BY period", nativeQuery = true)
    java.util.List<Object[]> aggregateMonthly(@Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to,
                @Param("hotelId") Long hotelId,
                @Param("method") String method);

    @Query(value = """
  SELECT h.name,
         COALESCE(SUM(p.total_price),0) AS amount,
         COUNT(*) AS cnt
  FROM hotel h
  JOIN room rm ON rm.hotel_id = h.id
  JOIN reservation r ON r.room_id = rm.id
  JOIN payment p ON p.reservation_id = r.id AND p.status = 'COMPLETED'
  WHERE p.created_at BETWEEN :from AND :to
    AND (:hotelId IS NULL OR h.id = :hotelId)
    AND (:method IS NULL OR p.payment_method = :method)
  GROUP BY h.name
  ORDER BY amount DESC
  """, nativeQuery = true)
    java.util.List<Object[]> aggregateByHotel(@Param("from") LocalDateTime from,
                @Param("to") LocalDateTime to,
                @Param("hotelId") Long hotelId,
                @Param("method") String method);

    @Query(value = """
  SELECT p.payment_method,
         COALESCE(SUM(p.total_price),0) AS amount,
         COUNT(*) AS cnt
  FROM payment p
  LEFT JOIN reservation r ON p.reservation_id = r.id
  LEFT JOIN room rm ON r.room_id = rm.id
  WHERE p.status = 'COMPLETED'
    AND p.created_at BETWEEN :from AND :to
    AND (:hotelId IS NULL OR rm.hotel_id = :hotelId)
    AND (:method IS NULL OR p.payment_method = :method)
  GROUP BY p.payment_method
  ORDER BY amount DESC
  """, nativeQuery = true)
    java.util.List<Object[]> aggregateByMethod(@Param("from") LocalDateTime from,
                 @Param("to") LocalDateTime to,
                 @Param("hotelId") Long hotelId,
                 @Param("method") String method);

    @Query(value = "SELECT COALESCE(SUM(p.total_price),0) FROM payment p " +
      "JOIN reservation r ON p.reservation_id = r.id " +
      "WHERE r.user_id = :userId AND p.status = 'COMPLETED'", nativeQuery = true)
    Long sumTotalCompletedByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT h.id, h.name, 0 AS revenue, 0 AS reservation_count, 0 AS room_count, 0 AS avg_rating " +
      "FROM hotel h " +
      "WHERE h.approval_status = 'APPROVED' " +
      "ORDER BY h.created_at DESC " +
      "LIMIT 10", nativeQuery = true)
    java.util.List<Object[]> simpleApprovedHotels();

    @Query(value = """
            SELECT
              h.id AS hotel_id,
              h.name AS hotel_name,
              COALESCE(SUM(p.total_price), 0) AS revenue,
              COUNT(DISTINCT res.id) AS reservation_count,
              COUNT(DISTINCT rm.id) AS room_count,
              COALESCE(AVG(rev.star_rating), 0) AS avg_rating
            FROM hotel h
            LEFT JOIN room rm ON rm.hotel_id = h.id
            LEFT JOIN reservation res ON res.room_id = rm.id
            LEFT JOIN payment p ON p.reservation_id = res.id AND p.status = 'COMPLETED'
            LEFT JOIN review rev ON rev.reservation_id = res.id
            WHERE h.approval_status = 'APPROVED'
            AND ( :year IS NULL OR :year <= 0 OR YEAR(p.created_at) = :year OR p.created_at IS NULL )
            GROUP BY h.id, h.name
            ORDER BY revenue DESC, h.created_at DESC
            """, nativeQuery = true)
    java.util.List<Object[]> fetchTopHotels(@Param("year") Integer year);
}
