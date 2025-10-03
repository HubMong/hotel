// src/main/java/com/example/hotel/search/dto/HotelSearchItem.java
package com.example.backend.hotel_search.dto;

public record HotelSearchItem(
        Long id,
        String name,
        String address,
        Integer starRating,
        Integer lowestPrice,
        String coverImage,
        String ratingLabel
) {}
