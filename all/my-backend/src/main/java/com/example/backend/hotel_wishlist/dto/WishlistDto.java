// src/main/java/com/example/backend/hotel_wishlist/dto/WishlistDto.java
package com.example.backend.hotel_wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishlistDto {
    private Long wishlistId;     
    private Long hotelId;
    private String hotelName;
    private String hotelAddress;
    private Integer hotelPrice;
    private String hotelImageUrl;
}