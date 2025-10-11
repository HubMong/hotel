package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.dto.AmenityOptionDto;
import com.example.backend.HotelOwner.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AmenityQueryController {

    private final AmenityRepository amenityRepository;

    /** 모든 편의시설 옵션 (체크박스 목록) */
    @GetMapping("/amenities")
    public ResponseEntity<List<AmenityOptionDto>> listAllAmenities() {
        var list = amenityRepository.findAll().stream()
                .map(a -> AmenityOptionDto.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .category(a.getCategory() != null ? a.getCategory().name() : null)
                        .build())
                .toList();
        return ResponseEntity.ok(list);
    }
}
