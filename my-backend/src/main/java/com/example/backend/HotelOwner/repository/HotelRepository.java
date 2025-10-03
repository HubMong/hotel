package com.example.backend.HotelOwner.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import com.example.backend.HotelOwner.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {

    // 오너별 호텔 (이미지까지 fetch)
    @Query("SELECT DISTINCT h FROM Hotel h LEFT JOIN FETCH h.images WHERE h.owner.id = :ownerId")
    List<Hotel> findByOwnerIdWithDetails(@Param("ownerId") Long ownerId);

    // ★ 권한체크: 호텔 id + 오너 email
    boolean existsByIdAndOwner_Email(Long id, String email);

    // 사업자 등록번호로 조회
    Optional<Hotel> findByBusinessId(Long businessId);

    // [ADDED] 관리자 목록용: owner를 즉시 로딩해서 LAZY 예외 방지 + DB 페이징/정렬
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Hotel> findAll(@Nullable Specification<Hotel> spec, Pageable pageable);

    @Query(value = "SELECT * FROM hotel h WHERE (:name IS NULL OR h.name LIKE CONCAT('%',:name,'%')) " +
	    "AND (:minStar IS NULL OR h.star_rating >= :minStar)",
	    countQuery = "SELECT COUNT(*) FROM hotel h WHERE (:name IS NULL OR h.name LIKE CONCAT('%',:name,'%')) " +
		    "AND (:minStar IS NULL OR h.star_rating >= :minStar)",
	    nativeQuery = true)
    Page<Hotel> search(@Param("name") String name, @Param("minStar") Integer minStar, Pageable pageable);

    @Query(value = "SELECT * FROM hotel h WHERE (:status IS NULL OR h.approval_status = :status) " +
	    "AND (:name IS NULL OR h.name LIKE CONCAT('%',:name,'%')) " +
	    "AND (:minStar IS NULL OR h.star_rating >= :minStar)",
	    countQuery = "SELECT COUNT(*) FROM hotel h WHERE (:status IS NULL OR h.approval_status = :status) " +
		    "AND (:name IS NULL OR h.name LIKE CONCAT('%',:name,'%')) " +
		    "AND (:minStar IS NULL OR h.star_rating >= :minStar)",
	    nativeQuery = true)
    Page<Hotel> searchByApproval(@Param("status") String status,
				 @Param("name") String name,
				 @Param("minStar") Integer minStar,
				 Pageable pageable);

    @Query(value = "SELECT h.id, h.name, bname, cnt, revenue, avg_rating FROM (\n" +
	    "  SELECT r.hotel_id as hotel_id, COUNT(res.id) as cnt, SUM(p.total_price) as revenue\n" +
	    "  FROM room r\n" +
	    "  JOIN reservation res ON res.room_id = r.id\n" +
	    "  LEFT JOIN payment p ON p.reservation_id = res.id\n" +
	    "  WHERE res.created_at BETWEEN :start AND :end\n" +
	    "  GROUP BY r.hotel_id\n" +
	    ") x\n" +
	    "JOIN hotel h ON h.id = x.hotel_id\n" +
	    "LEFT JOIN (SELECT reservation_id, AVG(star_rating) as avg_rating FROM review GROUP BY reservation_id) rv ON 1=1\n" +
	    "LEFT JOIN (SELECT id, name as bname FROM hotel) hb ON hb.id = h.id\n" +
	    "ORDER BY x.cnt DESC LIMIT 5",
	    nativeQuery = true)
    List<Object[]> getTopHotelsByReservations(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    // 사업자 정보와 통계 정보를 함께 가져오는 쿼리
    @Query(value = """
	    SELECT h.id, h.name, h.address, h.description, h.country, h.star_rating,
	           h.approval_status, h.approval_date, h.approved_by, h.rejection_reason, h.created_at,
	           u.name as business_name, u.email as business_email, u.phone as business_phone,
	           COALESCE(room_stats.room_count, 0) as room_count,
	           COALESCE(res_stats.reservation_count, 0) as reservation_count,
	           COALESCE(rating_stats.avg_rating, 0.0) as average_rating,
	           COALESCE(revenue_stats.total_revenue, 0) as total_revenue
	    FROM hotel h
	    LEFT JOIN app_user u ON h.user_id = u.id
	    LEFT JOIN (SELECT hotel_id, COUNT(*) as room_count FROM room GROUP BY hotel_id) room_stats ON h.id = room_stats.hotel_id
	    LEFT JOIN (SELECT r.hotel_id, COUNT(res.id) as reservation_count
	               FROM room r JOIN reservation res ON r.id = res.room_id
	               GROUP BY r.hotel_id) res_stats ON h.id = res_stats.hotel_id
	    LEFT JOIN (SELECT r.hotel_id, AVG(rev.star_rating) as avg_rating
	               FROM room r JOIN reservation res ON r.id = res.room_id
	               JOIN review rev ON res.id = rev.reservation_id
	               GROUP BY r.hotel_id) rating_stats ON h.id = rating_stats.hotel_id
	    LEFT JOIN (SELECT r.hotel_id, SUM(p.total_price) as total_revenue
	               FROM room r JOIN reservation res ON r.id = res.room_id
	               JOIN payment p ON res.id = p.reservation_id AND p.status = 'COMPLETED'
	               GROUP BY r.hotel_id) revenue_stats ON h.id = revenue_stats.hotel_id
	    WHERE (:status IS NULL OR h.approval_status = :status)
	    AND (:name IS NULL OR h.name LIKE CONCAT('%', :name, '%'))
	    AND (:minStar IS NULL OR h.star_rating >= :minStar)
	    ORDER BY h.id DESC
	    """, nativeQuery = true)
    List<Object[]> findHotelsWithBusinessInfo(@Param("status") String status,
                                            @Param("name") String name,
                                            @Param("minStar") Integer minStar);

    // 특정 호텔의 상세 통계 정보
    @Query(value = """
	    SELECT
	        COALESCE((SELECT COUNT(*) FROM room WHERE hotel_id = h.id), 0) as total_rooms,
	        COALESCE((SELECT COUNT(*) FROM reservation res JOIN room r ON res.room_id = r.id WHERE r.hotel_id = h.id), 0) as total_reservations,
	        COALESCE((SELECT AVG(rev.star_rating) FROM review rev JOIN reservation res ON rev.reservation_id = res.id
	         JOIN room r ON res.room_id = r.id WHERE r.hotel_id = h.id), 0.0) as average_rating,
	        COALESCE((SELECT SUM(p.total_price) FROM payment p JOIN reservation res ON p.reservation_id = res.id
	         JOIN room r ON res.room_id = r.id WHERE r.hotel_id = h.id AND p.status = 'COMPLETED'), 0) as total_revenue
	    FROM hotel h WHERE h.id = :hotelId
	    """, nativeQuery = true)
    Object[] findHotelStats(@Param("hotelId") Long hotelId);

    // 승인 상태별 카운트
    long countByApprovalStatus(Hotel.ApprovalStatus status);
}
