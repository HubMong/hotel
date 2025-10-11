package com.example.backend.HotelOwner.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateHotelRequest {
    private String name;
    private String address;
    private Integer starRating;
    private String description;

    private Double lat;
    private Double lng;
    private String thumbnailUrl;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Amenities {
        private List<String> left;
        private List<String> right;
    }
    private Amenities amenities;
    private List<String> highlightKeys;
}
