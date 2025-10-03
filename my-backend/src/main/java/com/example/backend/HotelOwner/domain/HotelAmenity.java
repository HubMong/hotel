package com.example.backend.HotelOwner.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotel_amenity")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class HotelAmenity {

    @EmbeddedId
    private HotelAmenityId id;

    @MapsId("hotelId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonIgnore                // 순환참조 차단 (Hotel -> list -> this -> Hotel ...)
    private Hotel hotel;

    @MapsId("amenityId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenity amenity;
}
