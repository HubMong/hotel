// src/main/java/com/example/backend/hotel_search/dto/HotelProjectionOnly.java
package com.example.backend.hotel_search.dto;

public interface HotelProjectionOnly {
    Long getId();
    String getName();
    String getAddress();
    Integer getStarRating();
    Integer getLowestPrice();   // 프런트가 기대하는 이름
    String getCoverImage();     // 프런트가 기대하는 이름(기존 thumbnailUrl → coverImage)
}
