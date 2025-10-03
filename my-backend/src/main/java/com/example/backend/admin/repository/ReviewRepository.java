package com.example.backend.admin.repository;

import com.example.backend.admin.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
	@Query("SELECT r FROM Review r WHERE (:reported IS NULL OR r.isReported = :reported) " +
			"AND (:hidden IS NULL OR r.isHidden = :hidden)")
	Page<Review> search(@Param("reported") Boolean reported,
	                   @Param("hidden") Boolean hidden,
	                   Pageable pageable);

	// 리뷰 상세 정보 조회 (호텔, 사용자, 예약 정보 포함)
	@Query(value = """
		SELECT
			rv.id AS reviewId,
			rv.star_rating AS starRating,
			rv.content AS content,
			rv.image AS image,
			rv.wrote_on AS wroteOn,
			rv.is_reported AS isReported,
			rv.is_hidden AS isHidden,
			rv.admin_reply AS adminReply,
			rv.replied_at AS repliedAt,
			
			r.id AS reservationId,
			r.transaction_id AS transactionId,
			
			h.id AS hotelId,
			h.name AS hotelName,
			
			u.id AS userId,
			u.name AS userName,
			u.email AS userEmail
		FROM review rv
		LEFT JOIN reservation r ON rv.reservation_id = r.id
		LEFT JOIN room rm ON r.room_id = rm.id
		LEFT JOIN hotel h ON rm.hotel_id = h.id
		LEFT JOIN app_user u ON r.user_id = u.id
		WHERE (:reported IS NULL OR rv.is_reported = :reported)
		AND (:hidden IS NULL OR rv.is_hidden = :hidden)
		AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
		AND (:userName IS NULL OR u.name LIKE CONCAT('%', :userName, '%'))
		""",
		countQuery = """
		SELECT COUNT(*)
		FROM review rv
		LEFT JOIN reservation r ON rv.reservation_id = r.id
		LEFT JOIN room rm ON r.room_id = rm.id
		LEFT JOIN hotel h ON rm.hotel_id = h.id
		LEFT JOIN app_user u ON r.user_id = u.id
		WHERE (:reported IS NULL OR rv.is_reported = :reported)
		AND (:hidden IS NULL OR rv.is_hidden = :hidden)
		AND (:hotelName IS NULL OR h.name LIKE CONCAT('%', :hotelName, '%'))
		AND (:userName IS NULL OR u.name LIKE CONCAT('%', :userName, '%'))
		""",
		nativeQuery = true)
	Page<Object[]> searchWithDetails(@Param("reported") Boolean reported,
	                               @Param("hidden") Boolean hidden,
	                               @Param("hotelName") String hotelName,
	                               @Param("userName") String userName,
	                               Pageable pageable);

	// 단일 리뷰 상세 조회
	@Query(value = """
		SELECT
			rv.id AS reviewId,
			rv.star_rating AS starRating,
			rv.content AS content,
			rv.image AS image,
			rv.wrote_on AS wroteOn,
			rv.is_reported AS isReported,
			rv.is_hidden AS isHidden,
			rv.admin_reply AS adminReply,
			rv.replied_at AS repliedAt,
			r.id AS reservationId,
			r.transaction_id AS transactionId,
			h.id AS hotelId,
			h.name AS hotelName,
			u.id AS userId,
			u.name AS userName,
			u.email AS userEmail
		FROM review rv
		LEFT JOIN reservation r ON rv.reservation_id = r.id
		LEFT JOIN room rm ON r.room_id = rm.id
		LEFT JOIN hotel h ON rm.hotel_id = h.id
		LEFT JOIN app_user u ON r.user_id = u.id
		WHERE rv.id = :reviewId
		""", nativeQuery = true)
	Object[] findDetailById(@Param("reviewId") Long reviewId);
}