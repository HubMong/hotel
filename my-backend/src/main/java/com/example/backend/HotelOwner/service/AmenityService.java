// my-backend/src/main/java/com/example/backend/service/AmenityService.java
package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.dto.AmenityDto;
import com.example.backend.HotelOwner.repository.AmenityRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public List<AmenityDto> getAllAmenities() {
        return amenityRepository.findAll().stream()
                .map(AmenityDto::fromEntity)
                .collect(Collectors.toList());
    }
}