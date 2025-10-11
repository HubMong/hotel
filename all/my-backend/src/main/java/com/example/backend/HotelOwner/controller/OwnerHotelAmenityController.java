package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelAmenity;
import com.example.backend.HotelOwner.domain.HotelAmenityId;
import com.example.backend.HotelOwner.repository.AmenityRepository;
import com.example.backend.HotelOwner.repository.HotelAmenityRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.UserRepository;
import lombok.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owner/hotels")
public class OwnerHotelAmenityController {

    private final HotelRepository hotelRepository;
    private final AmenityRepository amenityRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final UserRepository userRepository;

    /** 이 호텔에 연결된 편의시설 ID 목록 */
    @GetMapping("/{hotelId}/amenities")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<List<Long>> getHotelAmenityIds(@PathVariable Long hotelId, Authentication auth) {
        var owner = requireOwner(auth);
        var h = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "호텔 없음: id=" + hotelId));

        var ids = hotelAmenityRepository.findAmenitiesByHotel_Id(h.getId())
                .stream().map(Amenity::getId).toList();
        return ResponseEntity.ok(ids);
    }

    /** 이 호텔의 편의시설을 “교체 저장” (amenityIds 전체를 받아 기존 연결 삭제 후 재연결) */
    @PutMapping("/{hotelId}/amenities")
    @PreAuthorize("hasRole('BUSINESS')")
    @Transactional
    public ResponseEntity<Void> replaceHotelAmenities(@PathVariable Long hotelId,
                                                      @RequestBody Map<String, List<Long>> body,
                                                      Authentication auth) {
        var owner = requireOwner(auth);
        var h = hotelRepository.findByIdAndOwnerId(hotelId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "호텔 없음: id=" + hotelId));

        var amenityIds = body.getOrDefault("amenityIds", List.of());

        // 기존 연결 삭제
        hotelAmenityRepository.deleteByHotel_Id(h.getId());

        // 새로 연결
        if (!amenityIds.isEmpty()) {
            var amenityMap = amenityRepository.findAllById(amenityIds).stream()
                    .collect(java.util.stream.Collectors.toMap(Amenity::getId, a -> a));

            for (Long aid : amenityIds) {
                var a = amenityMap.get(aid);
                if (a == null) continue;
                var link = HotelAmenity.builder()
                        .id(new HotelAmenityId(h.getId(), a.getId()))
                        .hotel(h)
                        .amenity(a)
                        .build();
                hotelAmenityRepository.save(link);
            }
        }
        return ResponseEntity.noContent().build();
    }

    // ───────────────────────────
    private User requireOwner(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자 없음: " + email));
    }
}
