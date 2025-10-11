// src/main/java/com/example/backend/HotelOwner/dto/CreateHotelRequest.java
package com.example.backend.HotelOwner.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateHotelRequest {
    @NotBlank private String name;
    @NotBlank private String address;
    @NotBlank private String country;
    @Min(0) @Max(5) private int starRating;
    private Long businessId; // optional
}
