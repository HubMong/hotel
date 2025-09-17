package com.example.backend.domain;

import jakarta.persistence.*;
import com.example.backend.domain.HotelAmenityId;

@Entity
@Table(name = "hotel_amenity")
public class HotelAmenity {

    @EmbeddedId
    private HotelAmenityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("hotelId")
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("amenityId")
    @JoinColumn(name = "amenity_id")
    private Amenity amenity;

    public HotelAmenity() {}

    // Getter and Setter methods
    public HotelAmenityId getId() { return id; }
    public void setId(HotelAmenityId id) { this.id = id; }
    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
    public Amenity getAmenity() { return amenity; }
    public void setAmenity(Amenity amenity) { this.amenity = amenity; }
}