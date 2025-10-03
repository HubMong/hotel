// src/main/java/com/example/backend/admin/repository/AdminPaymentRepository.java
package com.example.backend.admin.repository;

import com.example.backend.payment.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminPaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
        SELECT p FROM Payment p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:from IS NULL OR p.createdAt >= :from)
          AND (:to   IS NULL OR p.createdAt <= :to)
        """)
    Page<Payment> search(
            @Param("status") Payment.Status status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    // 상세 조인 조회 (Native)
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
          AND (:to   IS NULL OR p.created_at <= :to)
          AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
          AND (:userName  IS NULL OR u.name LIKE CONCAT('%', :userName,  '%'))
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
          AND (:to   IS NULL OR p.created_at <= :to)
          AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
          AND (:userName  IS NULL OR u.name LIKE CONCAT('%', :userName,  '%'))
        """,
        nativeQuery = true)
    Page<Object[]> searchWithDetails(
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("hotelName") String hotelName,
            @Param("userName") String userName,
            Pageable pageable
    );

    // 월별 매출 합계
    @Query(value = """
        SELECT MONTH(created_at) AS m, COALESCE(SUM(total_price),0) AS revenue, COUNT(*) AS cnt
        FROM payment
        WHERE (:year IS NULL OR YEAR(created_at)=:year)
        GROUP BY m ORDER BY m
        """, nativeQuery = true)
    List<Object[]> sumMonthlyRevenue(@Param("year") Integer year);

    // 연도별 호텔 매출 (COMPLETED만)
    @Query(value = """
        SELECT h.id, h.name, COALESCE(SUM(p.total_price),0) AS revenue
        FROM hotel h
        JOIN room r ON r.hotel_id = h.id
        JOIN reservation res ON res.room_id = r.id
        JOIN payment p ON p.reservation_id = res.id AND p.status = 'COMPLETED'
        WHERE (:year IS NULL OR YEAR(p.created_at) = :year)
        GROUP BY h.id, h.name
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> hotelRevenueByYear(@Param("year") Integer year);

    // 기간 매출 합계 (상태 파라미터로 안전하게 비교)
    @Query("""
        SELECT COALESCE(SUM(p.totalPrice), 0)
        FROM Payment p
        WHERE p.createdAt BETWEEN :from AND :to
          AND p.status = :status
        """)
    Long sumTotalPriceByCreatedAtBetween(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("status") Payment.Status status);

    // COMPLETED 고정 헬퍼 (필요하면 사용)
    default Long sumCompletedTotalPriceByCreatedAtBetween(LocalDateTime from, LocalDateTime to) {
        return sumTotalPriceByCreatedAtBetween(from, to, Payment.Status.COMPLETED);
    }

    // 전체 결제 건수
    @Query("SELECT COUNT(p) FROM Payment p")
    Long countAllPayments();

    // 예약별 결제 목록
    @Query("SELECT p FROM Payment p WHERE p.reservationId = :reservationId")
    List<Payment> findByReservationId(@Param("reservationId") Long reservationId);

    // 최근 N일 일별 매출 (COMPLETED만)
    @Query(value = """
        SELECT DATE(p.created_at) as d, COALESCE(SUM(p.total_price),0) as revenue
        FROM payment p
        WHERE p.status='COMPLETED' AND p.created_at BETWEEN :from AND :to
        GROUP BY d
        ORDER BY d
        """, nativeQuery = true)
    List<Object[]> dailyRevenue(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    // 연도별 호텔 매출 TOP (상세 포함, 승인 호텔)
    @Query(value = """
        SELECT h.id, h.name,
               COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.total_price ELSE 0 END),0) AS revenue,
               COUNT(DISTINCT res.id) AS reservation_count,
               COUNT(DISTINCT r.id) AS room_count,
               COALESCE(AVG(rev.star_rating),0) AS avg_rating
        FROM hotel h
        LEFT JOIN room r ON r.hotel_id = h.id
        LEFT JOIN reservation res ON res.room_id = r.id
        LEFT JOIN payment p ON p.reservation_id = res.id
        LEFT JOIN review rev ON rev.reservation_id = res.id
        WHERE h.approval_status = 'APPROVED'
          AND (:year IS NULL OR p.created_at IS NULL OR YEAR(p.created_at)=:year)
        GROUP BY h.id, h.name
        HAVING COUNT(r.id) > 0
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> topHotelRevenue(@Param("year") Integer year);

    // 승인된 호텔 목록(매출 0 노출)
    @Query(value = """
        SELECT h.id, h.name, 0 AS revenue,
               COUNT(DISTINCT res.id) AS reservation_count,
               COUNT(DISTINCT r.id) AS room_count,
               0 AS avg_rating
        FROM hotel h
        LEFT JOIN room r ON r.hotel_id = h.id
        LEFT JOIN reservation res ON res.room_id = r.id
        WHERE h.approval_status = 'APPROVED'
        GROUP BY h.id, h.name
        ORDER BY h.id DESC
        """, nativeQuery = true)
    List<Object[]> approvedHotelsWithZeroRevenue();

    // 기간별 호텔 매출 (COMPLETED만)
    @Query(value = """
        SELECT h.id AS hotel_id, h.name AS hotel_name,
               COALESCE(SUM(p.total_price),0) AS revenue,
               COUNT(DISTINCT res.id) AS reservation_count
        FROM hotel h
        JOIN room r ON r.hotel_id = h.id
        JOIN reservation res ON res.room_id = r.id
        JOIN payment p ON p.reservation_id = res.id AND p.status='COMPLETED'
        WHERE p.created_at BETWEEN :from AND :to
        GROUP BY h.id, h.name
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<Object[]> hotelRevenueBetween(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);
}
