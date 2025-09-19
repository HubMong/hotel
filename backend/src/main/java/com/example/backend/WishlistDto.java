package com.example.backend;

import lombok.Data;

@Data
public class WishlistDto {
    private Long hotelId;
    private String hotelName;
    private String hotelImageUrl;
}