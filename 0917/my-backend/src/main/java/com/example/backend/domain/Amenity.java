package com.example.backend.domain;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "Amenity")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    // 편의시설 이름, 설명 등 필드
    private String name;
    private String description;

    // HotelAmenity와 양방향 연관관계
    @OneToMany(mappedBy = "amenity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HotelAmenity> hotelAmenities;

    // Getter / Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<HotelAmenity> getHotelAmenities() { return hotelAmenities; }
    public void setHotelAmenities(List<HotelAmenity> hotelAmenities) { this.hotelAmenities = hotelAmenities; }
}
