package com.example.backend.hotel_search.repository;

import com.example.backend.fe_hotel_detail.domain.Hotel; // 네 엔티티 경로 유지
import com.example.backend.hotel_search.dto.HotelProjectionOnly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelSearchRepository extends JpaRepository<Hotel, Long> {

    @Query(value = """
        SELECT
            h.id                                  AS id,
            h.name                                AS name,
            h.address                             AS city,          -- 표시용: 주소를 city로
            h.country                             AS country,
            CAST(h.star_rating AS DOUBLE)         AS rating,
            MIN(rpp.price)                        AS lowestPrice,   -- ★ 최저가
            ( SELECT hi.url
              FROM hotel_image hi
              WHERE hi.hotel_id = h.id
              ORDER BY hi.is_cover DESC, hi.sort_no ASC
              LIMIT 1
            )                                     AS thumbnailUrl
        FROM `Hotel` h
        LEFT JOIN `Room` r
               ON r.hotel_id = h.id
        LEFT JOIN `Room_Price_Policy` rpp
               ON rpp.room_id = r.id
              AND (
                    :checkIn IS NULL OR :checkOut IS NULL
                    OR ( rpp.start_date <= DATE(:checkOut) AND rpp.end_date >= DATE(:checkIn) )
                  )
        WHERE
            ( :q IS NULL OR :q = '' OR
              h.name    LIKE CONCAT('%', :q, '%') OR
              h.address LIKE CONCAT('%', :q, '%') OR
              h.country LIKE CONCAT('%', :q, '%')
            )
            -- 인원: adults+children 총합 기준
            AND (
              (:adults IS NULL AND :children IS NULL)
              OR EXISTS (
                   SELECT 1
                   FROM `Room` rx
                   WHERE rx.hotel_id = h.id
                     AND rx.capacity_max >= COALESCE(:adults,0) + COALESCE(:children,0)
                     AND rx.capacity_min <= GREATEST(COALESCE(:adults,0) + COALESCE(:children,0), 1)
              )
            )
            -- 가격 하한
            AND (
              :minPrice IS NULL
              OR EXISTS (
                   SELECT 1
                   FROM `Room_Price_Policy` px
                   JOIN `Room` r2 ON r2.id = px.room_id
                   WHERE r2.hotel_id = h.id
                     AND (
                          :checkIn IS NULL OR :checkOut IS NULL
                          OR ( px.start_date <= DATE(:checkOut) AND px.end_date >= DATE(:checkIn) )
                     )
                     AND px.price >= :minPrice
              )
            )
            -- 가격 상한
            AND (
              :maxPrice IS NULL
              OR EXISTS (
                   SELECT 1
                   FROM `Room_Price_Policy` py
                   JOIN `Room` r3 ON r3.id = py.room_id
                   WHERE r3.hotel_id = h.id
                     AND (
                          :checkIn IS NULL OR :checkOut IS NULL
                          OR ( py.start_date <= DATE(:checkOut) AND py.end_date >= DATE(:checkIn) )
                     )
                     AND py.price <= :maxPrice
              )
            )
        GROUP BY h.id, h.name, h.address, h.country, h.star_rating
        ORDER BY (MIN(rpp.price) IS NULL), MIN(rpp.price) ASC, h.id DESC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT h.id)
        FROM `Hotel` h
        LEFT JOIN `Room` r ON r.hotel_id = h.id
        LEFT JOIN `Room_Price_Policy` rpp
               ON rpp.room_id = r.id
              AND (
                    :checkIn IS NULL OR :checkOut IS NULL
                    OR ( rpp.start_date <= DATE(:checkOut) AND rpp.end_date >= DATE(:checkIn) )
                  )
        WHERE
            ( :q IS NULL OR :q = '' OR
              h.name    LIKE CONCAT('%', :q, '%') OR
              h.address LIKE CONCAT('%', :q, '%') OR
              h.country LIKE CONCAT('%', :q, '%')
            )
            AND (
              (:adults IS NULL AND :children IS NULL)
              OR EXISTS (
                   SELECT 1
                   FROM `Room` rx
                   WHERE rx.hotel_id = h.id
                     AND rx.capacity_max >= COALESCE(:adults,0) + COALESCE(:children,0)
                     AND rx.capacity_min <= GREATEST(COALESCE(:adults,0) + COALESCE(:children,0), 1)
              )
            )
            AND (
              :minPrice IS NULL
              OR EXISTS (
                   SELECT 1
                   FROM `Room_Price_Policy` px
                   JOIN `Room` r2 ON r2.id = px.room_id
                   WHERE r2.hotel_id = h.id
                     AND (
                          :checkIn IS NULL OR :checkOut IS NULL
                          OR ( px.start_date <= DATE(:checkOut) AND px.end_date >= DATE(:checkIn) )
                     )
                     AND px.price >= :minPrice
              )
            )
            AND (
              :maxPrice IS NULL
              OR EXISTS (
                   SELECT 1
                   FROM `Room_Price_Policy` py
                   JOIN `Room` r3 ON r3.id = py.room_id
                   WHERE r3.hotel_id = h.id
                     AND (
                          :checkIn IS NULL OR :checkOut IS NULL
                          OR ( py.start_date <= DATE(:checkOut) AND py.end_date >= DATE(:checkIn) )
                     )
                     AND py.price <= :maxPrice
              )
            )
        """,
        nativeQuery = true)
    Page<HotelProjectionOnly> search(
            @Param("q") String q,
            @Param("checkIn") String checkIn,     // 'YYYY-MM-DD' 권장
            @Param("checkOut") String checkOut,   // 'YYYY-MM-DD' 권장
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("adults") Integer adults,
            @Param("children") Integer children,
            Pageable pageable
    );
}
