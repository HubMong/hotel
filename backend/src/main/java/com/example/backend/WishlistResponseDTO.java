package com.example.backend;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WishlistResponseDTO {

    private Long wishlistId;
    private Long hotelId;
    private String hotelName;
    private String hotelImageUrl; 
}
