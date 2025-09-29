// my-backend/src/main/java/com/example/backend/dto/AmenityDto.java
package com.example.backend.HotelOwner.dto;

import com.example.backend.HotelOwner.domain.Amenity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmenityDto {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private Amenity.Category category;

    public static AmenityDto fromEntity(Amenity amenity) {
        return AmenityDto.builder()
                .id(amenity.getId())
                .name(amenity.getName())
                .description(amenity.getDescription())
                .iconUrl(amenity.getIconUrl())
                .category(amenity.getCategory())
                .build();
    }
}