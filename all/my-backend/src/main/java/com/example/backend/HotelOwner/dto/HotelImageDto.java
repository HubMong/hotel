// src/main/java/com/example/backend/HotelOwner/dto/HotelImageDto.java
package com.example.backend.HotelOwner.dto;

import com.example.backend.HotelOwner.domain.HotelImage;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HotelImageDto {
    private Long id;
    private String url;
    private boolean cover;
    private int sortNo;
    private String caption;

    public static HotelImageDto from(HotelImage e) {
        return HotelImageDto.builder()
                .id(e.getId())
                .url(e.getUrl())
                .cover(e.isCover())
                .sortNo(e.getSortNo())
                .caption(e.getCaption())
                .build();
    }
}
