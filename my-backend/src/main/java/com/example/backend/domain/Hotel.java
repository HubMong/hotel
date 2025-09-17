// src/main/java/com/example/backend/domain/Hotel.java

package com.example.backend.domain;

import jakarta.persistence.*;
import java.util.List;
import java.util.Arrays;

@Entity
@Table(name = "hotel") // ✅ DB 테이블명과 동일하게 소문자로 수정
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms;

    private String name;
    private String address;

    @Column(name = "star_rating") // ✅ DB 컬럼명 명시
    private String starRating;

    private String image;
    private String country;

    @Transient
    private Integer capacity;

    @Transient
    private List<String> amenities;

    public Hotel() {}

    public Hotel(int id, String name, String address, String starRating, String image, String country, Integer capacity, String amenityNames) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.starRating = starRating;
        this.image = image;
        this.country = country;
        this.capacity = capacity;
        if (amenityNames != null && !amenityNames.isEmpty()) {
            // ✅ Java 8 호환성 위해 Arrays.asList() 사용
            this.amenities = Arrays.asList(amenityNames.split(","));
        }
    }

    // Getter 및 Setter 메소드
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStarRating() { return starRating; }
    public void setStarRating(String starRating) { this.starRating = starRating; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
}
