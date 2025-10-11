// src/main/java/com/example/backend/HotelOwner/repository/HotelAmenityRepository.java
package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.domain.HotelAmenity;
import com.example.backend.HotelOwner.domain.HotelAmenityId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenity, HotelAmenityId> {

    @Query("select ha.amenity from HotelAmenity ha where ha.hotel.id = :hotelId")
    List<Amenity> findAmenitiesByHotel_Id(@Param("hotelId") Long hotelId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByHotel_Id(Long hotelId);

    boolean existsByHotel_IdAndAmenity_Id(Long hotelId, Long amenityId);
}
