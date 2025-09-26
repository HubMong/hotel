package com.example.backend.fe_hotel_detail.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomDto {
    private Long id;
    private String name;
    private Integer size;
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
    private Integer lastBookedHours;
    private List<String> photos;                     // ← 서비스에서 주입
    private List<Map<String, String>> promos;        // ← 서비스에서 주입
    private Integer qty;
}
