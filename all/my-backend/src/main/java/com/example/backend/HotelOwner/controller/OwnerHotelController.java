// src/main/java/com/example/backend/HotelOwner/controller/OwnerHotelController.java
package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.dto.CreateHotelRequest;
import com.example.backend.HotelOwner.dto.OwnerHotelDetailDto;
import com.example.backend.HotelOwner.dto.OwnerHotelDto;
import com.example.backend.HotelOwner.dto.OwnerHotelUpdateRequest;
import com.example.backend.HotelOwner.repository.*;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/owner/hotels")
@RequiredArgsConstructor
public class OwnerHotelController {

    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final AmenityRepository amenityRepository;

    private final RoomRepository roomRepository;
    private final OwnerReservationRepository ownerReservationRepository;
    private final HotelImageRepository hotelImageRepository;

    @GetMapping("/my-hotels")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<List<OwnerHotelDto>> myHotels(Authentication auth) {
        String email = auth.getName();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다: " + email));

        var list = hotelRepository.findByOwnerIdWithDetails(owner.getId()).stream()
                .map(this::toSimpleDto)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasRole('BUSINESS')")
    @Transactional
    public ResponseEntity<OwnerHotelDto> create(@Valid @RequestBody CreateHotelRequest req, Authentication auth) {
        String email = auth.getName();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다: " + email));

        Hotel h = Hotel.builder()
                .owner(owner)
                .businessId(req.getBusinessId())
                .name(req.getName())
                .address(req.getAddress())
                .country(req.getCountry())
                .starRating(req.getStarRating())
                // 초기 상태는 정책에 맞게 (예: PENDING)
                .approvalStatus(Hotel.ApprovalStatus.PENDING)
                .build();

        Hotel saved = hotelRepository.save(h);
        var body = toSimpleDto(saved);
        return ResponseEntity.created(URI.create("/api/owner/hotels/" + saved.getId())).body(body);
    }

    // 상세
    @GetMapping("/my-hotels/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<OwnerHotelDetailDto> getMine(@PathVariable Long id, Authentication auth) {
        Hotel h = getMyHotelOr404(id, auth);

        List<String> names = hotelAmenityRepository.findAmenitiesByHotel_Id(h.getId())
                .stream().map(a -> a.getName()).toList();
        List<String> left = new ArrayList<>(), right = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            (i % 2 == 0 ? left : right).add(names.get(i));
        }

        OwnerHotelDetailDto dto = OwnerHotelDetailDto.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .starRating(h.getStarRating())
                .description(h.getDescription())
                .amenities(new OwnerHotelDetailDto.Amenities(left, right))
                .approvalStatus(h.getApprovalStatus() != null ? h.getApprovalStatus().name() : "PENDING")
                .build();

        return ResponseEntity.ok(dto);
    }

    // 수정
    @PutMapping("/my-hotels/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    @Transactional
    public ResponseEntity<OwnerHotelDetailDto> updateMine(@PathVariable Long id,
                                                          @RequestBody OwnerHotelUpdateRequest req,
                                                          Authentication auth) {
        Hotel h = getMyHotelOr404(id, auth);

        if (req.getName() != null)        h.setName(req.getName());
        if (req.getAddress() != null)     h.setAddress(req.getAddress());
        if (req.getStarRating() != null)  h.setStarRating(req.getStarRating());
        if (req.getDescription() != null) h.setDescription(req.getDescription());
        // lat/lng/thumbnail/highlightKeys 반영 제거

        if (req.getAmenities() != null) {
            List<String> merged = new ArrayList<>();
            if (req.getAmenities().getLeft()  != null) merged.addAll(req.getAmenities().getLeft());
            if (req.getAmenities().getRight() != null) merged.addAll(req.getAmenities().getRight());
            List<String> unique = merged.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim).filter(s->!s.isEmpty())
                    .distinct()
                    .toList();

            hotelAmenityRepository.deleteByHotel_Id(h.getId());

            for (String name : unique) {
                var amenity = amenityRepository.findByName(name)
                        .orElseGet(() -> amenityRepository.save(
                                com.example.backend.HotelOwner.domain.Amenity.builder().name(name).build()
                        ));
                var link = com.example.backend.HotelOwner.domain.HotelAmenity.builder()
                        .id(new com.example.backend.HotelOwner.domain.HotelAmenityId(h.getId(), amenity.getId()))
                        .hotel(h)
                        .amenity(amenity)
                        .build();
                hotelAmenityRepository.save(link);
            }
        }

        hotelRepository.saveAndFlush(h);
        return getMine(id, auth);
    }

    @DeleteMapping("/my-hotels/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    @Transactional
    public ResponseEntity<Void> deleteMine(@PathVariable Long id, Authentication auth) {
        Hotel h = getMyHotelOr404(id, auth);

        if (ownerReservationRepository.countAnyByHotelId(h.getId()) > 0) {
            throw new ResponseStatusException(CONFLICT, "예약이 남아 있어 호텔을 삭제할 수 없습니다.");
        }
        if (roomRepository.existsByHotel_Id(h.getId())) {
            throw new ResponseStatusException(CONFLICT, "객실이 남아 있어 호텔을 삭제할 수 없습니다. 객실을 먼저 삭제하세요.");
        }

        hotelAmenityRepository.deleteByHotel_Id(h.getId());
        hotelImageRepository.deleteByHotel_Id(h.getId());
        hotelRepository.delete(h);

        return ResponseEntity.noContent().build();
    }

    // ===== util =====
    private Hotel getMyHotelOr404(Long id, Authentication auth) {
        String email = auth.getName();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자를 찾을 수 없습니다: " + email));

        return hotelRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "호텔을 찾을 수 없습니다: id=" + id));
    }

    private OwnerHotelDto toSimpleDto(Hotel h) {
        return OwnerHotelDto.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .starRating(h.getStarRating())
                .approvalStatus(h.getApprovalStatus().name())
                .build();
    }
}
