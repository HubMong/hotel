// src/main/java/com/example/backend/HotelOwner/repository/AmenityRepository.java
package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    // ✅ 컨트롤러에서 사용
    Optional<Amenity> findByName(String name);
}
