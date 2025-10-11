package com.example.backend.hotel_search.repository;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.hotel_search.dto.HotelProjectionOnly;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotelSearchRepository extends JpaRepository<Hotel, Long> {

    // ===== 정교 쿼리 (있으면 이거 사용)
    @Query(value = """
        SELECT
            t.id           AS id,
            t.name         AS name,
            t.address      AS address,
            t.star_rating  AS starRating,
            t.lowestPrice  AS lowestPrice,
            t.coverImage   AS coverImage
        FROM (
            SELECT
                h.id,
                h.name,
                h.address,
                h.star_rating,
                MIN(
                  CASE
                    WHEN COALESCE(r.status, 'ACTIVE') = 'ACTIVE'
                     AND COALESCE(r.room_count, 1) > 0
                    THEN COALESCE(rpp.price, r.price)
                    ELSE NULL
                  END
                ) AS lowestPrice,
                ( SELECT hi.url
                  FROM hotel_image hi
                  WHERE hi.hotel_id = h.id
                  ORDER BY hi.is_cover DESC, hi.sort_no ASC, hi.id ASC
                  LIMIT 1
                ) AS coverImage
            FROM hotel h
            LEFT JOIN room r
                   ON r.hotel_id = h.id
            LEFT JOIN room_price_policy rpp
                   ON rpp.room_id = r.id
                  AND ( :checkIn IS NULL OR :checkOut IS NULL
                        OR ( rpp.start_date <= DATE(:checkOut) AND rpp.end_date >= DATE(:checkIn) ) )
            WHERE
                h.approval_status = 'APPROVED'
                AND (
                  :q IS NULL OR :q = '' OR
                  LOWER(h.name)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.address) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.country) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                AND (
                  (:adults IS NULL AND :children IS NULL)
                  OR NOT EXISTS (SELECT 1 FROM room rx0 WHERE rx0.hotel_id = h.id)
                  OR EXISTS (
                       SELECT 1
                       FROM room rx
                       WHERE rx.hotel_id = h.id
                         AND COALESCE(rx.status, 'ACTIVE') = 'ACTIVE'
                         AND COALESCE(rx.room_count, 1) > 0
                         AND ( rx.capacity_max IS NULL
                               OR rx.capacity_max >= COALESCE(:adults,0) + COALESCE(:children,0) )
                         AND ( rx.capacity_min IS NULL
                               OR rx.capacity_min <= GREATEST(COALESCE(:adults,0) + COALESCE(:children,0), 1) )
                  )
                )
            GROUP BY h.id, h.name, h.address, h.star_rating
        ) t
        WHERE ( :minPrice IS NULL OR t.lowestPrice >= :minPrice )
          AND ( :maxPrice IS NULL OR t.lowestPrice <= :maxPrice )
        ORDER BY (t.lowestPrice IS NULL), t.lowestPrice ASC, t.id DESC
        """,
        countQuery = """
        SELECT COUNT(1) FROM (
            SELECT
                h.id,
                MIN(
                  CASE
                    WHEN COALESCE(r.status, 'ACTIVE') = 'ACTIVE'
                     AND COALESCE(r.room_count, 1) > 0
                    THEN COALESCE(rpp.price, r.price)
                    ELSE NULL
                  END
                ) AS lowestPrice
            FROM hotel h
            LEFT JOIN room r
                   ON r.hotel_id = h.id
            LEFT JOIN room_price_policy rpp
                   ON rpp.room_id = r.id
                  AND ( :checkIn IS NULL OR :checkOut IS NULL
                        OR ( rpp.start_date <= DATE(:checkOut) AND rpp.end_date >= DATE(:checkIn) ) )
            WHERE
                h.approval_status = 'APPROVED'
                AND (
                  :q IS NULL OR :q = '' OR
                  LOWER(h.name)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.address) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.country) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                AND (
                  (:adults IS NULL AND :children IS NULL)
                  OR NOT EXISTS (SELECT 1 FROM room rx0 WHERE rx0.hotel_id = h.id)
                  OR EXISTS (
                       SELECT 1
                       FROM room rx
                       WHERE rx.hotel_id = h.id
                         AND COALESCE(rx.status, 'ACTIVE') = 'ACTIVE'
                         AND COALESCE(rx.room_count, 1) > 0
                         AND ( rx.capacity_max IS NULL
                               OR rx.capacity_max >= COALESCE(:adults,0) + COALESCE(:children,0) )
                         AND ( rx.capacity_min IS NULL
                               OR rx.capacity_min <= GREATEST(COALESCE(:adults,0) + COALESCE(:children,0), 1) )
                  )
                )
            GROUP BY h.id
            HAVING ( :minPrice IS NULL OR MIN(
                        CASE
                          WHEN COALESCE(r.status,'ACTIVE')='ACTIVE' AND COALESCE(r.room_count,1)>0
                          THEN COALESCE(rpp.price, r.price) END
                    ) >= :minPrice )
               AND ( :maxPrice IS NULL OR MIN(
                        CASE
                          WHEN COALESCE(r.status,'ACTIVE')='ACTIVE' AND COALESCE(r.room_count,1)>0
                          THEN COALESCE(rpp.price, r.price) END
                    ) <= :maxPrice )
        ) c
        """,
        nativeQuery = true)
    Page<HotelProjectionOnly> search(
            @Param("q") String q,
            @Param("checkIn") String checkIn,
            @Param("checkOut") String checkOut,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("adults") Integer adults,
            @Param("children") Integer children,
            Pageable pageable
    );

    // ===== 심플 쿼리 (room_price_policy, status, room_count 없어도 동작)
    @Query(value = """
        SELECT
            t.id           AS id,
            t.name         AS name,
            t.address      AS address,
            t.star_rating  AS starRating,
            t.lowestPrice  AS lowestPrice,
            t.coverImage   AS coverImage
        FROM (
            SELECT
                h.id,
                h.name,
                h.address,
                h.star_rating,
                MIN(r.price) AS lowestPrice,
                ( SELECT hi.url
                  FROM hotel_image hi
                  WHERE hi.hotel_id = h.id
                  ORDER BY hi.is_cover DESC, hi.sort_no ASC, hi.id ASC
                  LIMIT 1
                ) AS coverImage
            FROM hotel h
            LEFT JOIN room r ON r.hotel_id = h.id
            WHERE
                h.approval_status = 'APPROVED'
                AND (
                  :q IS NULL OR :q = '' OR
                  LOWER(h.name)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.address) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.country) LIKE LOWER(CONCAT('%', :q, '%'))
                )
            GROUP BY h.id, h.name, h.address, h.star_rating
        ) t
        WHERE ( :minPrice IS NULL OR t.lowestPrice >= :minPrice )
          AND ( :maxPrice IS NULL OR t.lowestPrice <= :maxPrice )
        ORDER BY (t.lowestPrice IS NULL), t.lowestPrice ASC, t.id DESC
        """,
        countQuery = """
        SELECT COUNT(1) FROM (
            SELECT h.id, MIN(r.price) AS lowestPrice
            FROM hotel h
            LEFT JOIN room r ON r.hotel_id = h.id
            WHERE
                h.approval_status = 'APPROVED'
                AND (
                  :q IS NULL OR :q = '' OR
                  LOWER(h.name)    LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.address) LIKE LOWER(CONCAT('%', :q, '%')) OR
                  LOWER(h.country) LIKE LOWER(CONCAT('%', :q, '%'))
                )
            GROUP BY h.id
            HAVING ( :minPrice IS NULL OR MIN(r.price) >= :minPrice )
               AND ( :maxPrice IS NULL OR MIN(r.price) <= :maxPrice )
        ) c
        """,
        nativeQuery = true)
    Page<HotelProjectionOnly> searchSimple(
            @Param("q") String q,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable
    );
}
