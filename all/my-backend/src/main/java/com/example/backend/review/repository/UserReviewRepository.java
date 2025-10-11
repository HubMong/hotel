package com.example.backend.review.repository;

import com.example.backend.review.domain.UserReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserReviewRepository extends JpaRepository<UserReview, Long> {

    // ✅ 특정 유저의 리뷰
    List<UserReview> findByReservation_UserId(Long userId);

    // ✅ 특정 호텔의 리뷰 (호텔 ID 기준)
    @Query("""
        SELECT r FROM UserReview r
        JOIN r.reservation res
        JOIN com.example.backend.HotelOwner.domain.Room room ON res.roomId = room.id
        WHERE room.hotel.id = :hotelId
    """)
    List<UserReview> findByHotelId(@Param("hotelId") Long hotelId);

    boolean existsByReservationId(Long reservationId);

}
