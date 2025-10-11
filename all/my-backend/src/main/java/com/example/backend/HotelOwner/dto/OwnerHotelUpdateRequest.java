// src/main/java/com/example/backend/HotelOwner/dto/OwnerHotelUpdateRequest.java
package com.example.backend.HotelOwner.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class OwnerHotelUpdateRequest {
    private String name;
    private String address;
    private Integer starRating;
    private String description;


    private Amenities amenities; // { left:[], right:[] }

    @Getter
    public static class Amenities {
        private List<String> left;
        private List<String> right;
    }
}
