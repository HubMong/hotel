package com.example.backend.hotel_support.repository;

import com.example.backend.hotel_support.domain.HotelInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HotelInquiryRepository extends JpaRepository<HotelInquiry, Long> {

    // ✅ 1. 사용자 본인의 문의 내역 조회 (모든 연관 객체 FETCH)
    @Query("""
        SELECT i FROM HotelInquiry i
        LEFT JOIN FETCH i.room r
        LEFT JOIN FETCH r.hotel h
        LEFT JOIN FETCH i.reservation res
        LEFT JOIN FETCH i.user u
        WHERE i.userId = :userId
        ORDER BY i.createdAt DESC
    """)
    List<HotelInquiry> findByUserIdWithDetails(@Param("userId") Long userId);

    // ✅ 2. 오너용 기본 쿼리 (Room, Hotel, User 모두 FETCH)
    @Query("""
        SELECT i FROM HotelInquiry i
        LEFT JOIN FETCH i.room r
        LEFT JOIN FETCH r.hotel h
        LEFT JOIN FETCH i.user u
        WHERE h.id IN :hotelIds
        ORDER BY i.createdAt DESC
    """)
    List<HotelInquiry> findInquiriesByHotelIds(@Param("hotelIds") List<Long> hotelIds);

    // ✅ 3. 오너용 UserName 필터 포함 쿼리 (User 이름으로 검색)
    @Query("""
        SELECT i FROM HotelInquiry i
        LEFT JOIN FETCH i.room r
        LEFT JOIN FETCH r.hotel h
        LEFT JOIN FETCH i.user u
        WHERE h.id IN :hotelIds
        AND (:userName IS NULL OR u.name LIKE %:userName%)
        ORDER BY i.createdAt DESC
    """)
    List<HotelInquiry> findInquiriesByHotelIdsAndUserName(
        @Param("hotelIds") List<Long> hotelIds,
        @Param("userName") String userName
    );

    // ✅ 4. 단순 조회용 (별도 FETCH 없이 기본 구조로 유지)
    List<HotelInquiry> findByUserIdOrderByCreatedAtDesc(Long userId);

    //🔑 [추가] 점주 소유 호텔 중 미답변(PENDING) 문의 개수를 반환합니다.
    @Query("SELECT COUNT(i) FROM HotelInquiry i WHERE i.hotelId IN :hotelIds AND i.status = 'PENDING'")
    long countPendingByHotelIds(@Param("hotelIds") List<Long> hotelIds);
}
