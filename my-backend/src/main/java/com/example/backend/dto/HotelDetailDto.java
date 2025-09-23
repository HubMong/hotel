package com.example.backend.dto;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HotelDetailDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Rating {
        private double score;
        private Map<String, Double> subs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Highlight {
        private String ic;
        private String title;
        private String sub;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Amenities {
        private List<String> left;
        private List<String> right;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HotelDto {
        private Long id;
        private String name;
        private String address;
        private String description;
        private List<String> images;   // 호텔 이미지 URL
        private List<String> badges;   // 간단 배지
        private Rating rating;         // 평점 요약
        private List<Highlight> highlights;
        private Amenities amenities;
        private String notice;         // 공지/알림
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoomDto {
        private Long id;
        private String name;
        private Integer size;      // m2 숫자
        private String view;
        private String bed;
        private Integer bath;
        private Boolean smoke;
        private Boolean sharedBath;
        private Boolean window;
        private Boolean aircon;
        private Boolean water;
        private Boolean wifi;
        private String cancelPolicy;
        private String payment;
        private Integer originalPrice;
        private Integer price;
        private Integer lastBookedHours;  // 데모용
        private List<String> photos;
        private List<Map<String, String>> promos;
        private Integer qty;              // 프런트 바인딩용
    }

    private HotelDto hotel;
    private List<RoomDto> rooms;
}
