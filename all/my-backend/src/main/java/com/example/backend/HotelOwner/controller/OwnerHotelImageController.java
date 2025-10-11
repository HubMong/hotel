// src/main/java/com/example/backend/HotelOwner/controller/OwnerHotelImageController.java
package com.example.backend.HotelOwner.controller;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelImage;
import com.example.backend.HotelOwner.dto.HotelImageDto;
import com.example.backend.HotelOwner.repository.HotelImageRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.service.FileStorageService;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/owner/hotels")
@RequiredArgsConstructor
public class OwnerHotelImageController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final FileStorageService fileStorageService;

    private Hotel getMyHotelOr404(Long id, Authentication auth) {
        String email = auth.getName();
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "사용자 없음"));
        return hotelRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "호텔 없음"));
    }

    @GetMapping("/my-hotels/{id}/images")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<List<HotelImageDto>> list(@PathVariable Long id, Authentication auth) {
        Hotel h = getMyHotelOr404(id, auth);
        var list = hotelImageRepository.findByHotelIdOrderBySortNoAsc(h.getId())
                .stream().map(HotelImageDto::from).toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping(value = "/my-hotels/{id}/images", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('BUSINESS')")
    @Transactional
    public ResponseEntity<List<HotelImageDto>> upload(@PathVariable Long id,
                                                      @RequestPart("images") List<MultipartFile> images,
                                                      Authentication auth) {
        Hotel h = getMyHotelOr404(id, auth);

        int baseSort = hotelImageRepository.findByHotelIdOrderBySortNoAsc(h.getId())
                .stream().mapToInt(HotelImage::getSortNo).max().orElse(-1);

        boolean alreadyHasCover = hotelImageRepository.findByHotelIdOrderBySortNoAsc(h.getId())
                .stream().anyMatch(HotelImage::isCover);

        for (int i = 0; i < images.size(); i++) {
            String url = fileStorageService.storeFile(images.get(i));
            boolean makeCover = (i == 0) && !alreadyHasCover;

            HotelImage img = HotelImage.builder()
                    .hotel(h)
                    .url(url)
                    .sortNo(baseSort + 1 + i)
                    .isCover(makeCover)
                    .build();
            hotelImageRepository.save(img);
        }
        var list = hotelImageRepository.findByHotelIdOrderBySortNoAsc(h.getId())
                .stream().map(HotelImageDto::from).toList();
        return ResponseEntity.status(CREATED).body(list);
    }

    @DeleteMapping("/my-hotels/{id}/images/{imageId}")
    @PreAuthorize("hasRole('BUSINESS')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable Long imageId, Authentication auth) {
        Hotel h = getMyHotelOr404(id, auth);
        var img = hotelImageRepository.findByIdAndHotel_Id(imageId, h.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "이미지 없음"));
        hotelImageRepository.delete(img);
        // 필요 시: fileStorageService.deleteFile(img.getUrl());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/my-hotels/{id}/images/{imageId}/cover")
    @PreAuthorize("hasRole('BUSINESS')")
    @Transactional
    public ResponseEntity<List<HotelImageDto>> setCover(@PathVariable Long id, @PathVariable Long imageId, Authentication auth) {
        Hotel h = getMyHotelOr404(id, auth);
        var img = hotelImageRepository.findByIdAndHotel_Id(imageId, h.getId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "이미지 없음"));

        var all = hotelImageRepository.findByHotelIdOrderBySortNoAsc(h.getId());
        all.forEach(i -> i.setCover(i.getId().equals(img.getId())));
        hotelImageRepository.saveAll(all);

        var list = all.stream().map(HotelImageDto::from).toList();
        return ResponseEntity.ok(list);
    }
}
