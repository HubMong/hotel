package com.example.backend.HotelOwner.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelImage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HotelDto {
    private Long id;
    private Long businessId;
    private String name;
    private String address;
    private Integer starRating;
    private String description;
    private String country;

    // 프론트 호환을 위해 필드명은 status 유지(값은 approvalStatus 기반)
    private String status;

    // 다중 이미지 URL
    private List<String> imageUrls;

    // 편의시설 ID 목록
    private List<Long> amenityIds;

    public static HotelDto fromEntity(Hotel hotel) {
        HotelDto dto = new HotelDto();
        dto.setId(hotel.getId());
        dto.setName(hotel.getName());
        dto.setAddress(hotel.getAddress());
        dto.setStarRating(hotel.getStarRating());
        dto.setDescription(hotel.getDescription());
        dto.setCountry(hotel.getCountry());
        dto.setBusinessId(hotel.getBusinessId());

        // ★ 변경: hotel.getStatus() → hotel.getApprovalStatus()
        dto.setStatus(
            hotel.getApprovalStatus() != null
                ? hotel.getApprovalStatus().name()
                : "PENDING"
        );

        if (hotel.getImages() != null) {
            dto.imageUrls = hotel.getImages().stream()
                    .map(HotelImage::getUrl)
                    .collect(Collectors.toList());
        }

        if (hotel.getHotelAmenities() != null) {
            dto.amenityIds = hotel.getHotelAmenities().stream()
                    .map(ha -> ha.getAmenity().getId())
                    .collect(Collectors.toList());
        }

        return dto;
    }
}
