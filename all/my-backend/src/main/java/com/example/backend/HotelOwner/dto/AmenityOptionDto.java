package com.example.backend.HotelOwner.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AmenityOptionDto {
    private Long id;
    private String name;
    private String category; // 필요 시 사용 (IN_ROOM, IN_HOTEL, ...)
}
