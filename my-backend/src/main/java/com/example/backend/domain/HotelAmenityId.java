package com.example.backend.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class HotelAmenityId implements Serializable {

    private int hotelId;
    private int amenityId;

    public HotelAmenityId() {}

    public HotelAmenityId(int hotelId, int amenityId) {
        this.hotelId = hotelId;
        this.amenityId = amenityId;
    }

    // Getter and Setter methods
    public int getHotelId() { return hotelId; }
    public void setHotelId(int hotelId) { this.hotelId = hotelId; }
    public int getAmenityId() { return amenityId; }
    public void setAmenityId(int amenityId) { this.amenityId = amenityId; }

    // hashCode and equals methods are required for composite keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HotelAmenityId that = (HotelAmenityId) o;
        return hotelId == that.hotelId && amenityId == that.amenityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hotelId, amenityId);
    }
}