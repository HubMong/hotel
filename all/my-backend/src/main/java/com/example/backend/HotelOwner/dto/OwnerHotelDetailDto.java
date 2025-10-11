// src/main/java/com/example/backend/HotelOwner/dto/OwnerHotelDetailDto.java
package com.example.backend.HotelOwner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OwnerHotelDetailDto {
    private Long id;
    private String name;
    private String address;
    private Integer starRating;
    private String description;


    private Amenities amenities;

    // 참고용: 상태 문자열
    private String approvalStatus;

    @Getter
    @AllArgsConstructor
    public static class Amenities {
        private List<String> left;
        private List<String> right;
    }
}
