// src/main/java/com/example/backend/repository/HotelRepository.java

package com.example.backend.repository;

import com.example.backend.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {

    @Query(value = "SELECT new com.example.backend.domain.Hotel(h.id, h.name, h.address, h.starRating, h.image, h.country, MAX(r.capacity_max), GROUP_CONCAT(a.name)) " + // <-- 'h.star_rating'을 'h.starRating'으로 수정
            "FROM Hotel h " +
            "JOIN Room r ON h.id = r.hotel.id " +
            "LEFT JOIN HotelAmenity ha ON h.id = ha.id.hotelId " +
            "LEFT JOIN Amenity a ON ha.amenity.id = a.id " +
            "WHERE (:destination IS NULL OR h.name LIKE CONCAT('%', :destination, '%')) " +
            "AND (:guests IS NULL OR r.capacity_max >= :guests) " +
            "GROUP BY h.id, h.name, h.address, h.starRating, h.image, h.country", // <-- 이 부분도 'h.starRating'으로 수정
            nativeQuery = false)
    List<Hotel> findHotelsWithFilter(@Param("destination") String destination, @Param("guests") Integer guests);
}