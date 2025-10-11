package com.example.backend.HotelOwner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class HotelAmenityId implements Serializable {
    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(name = "amenity_id", nullable = false)
    private Long amenityId;
}
