package com.example.backend.HotelOwner.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "amenity")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이름은 유니크 권장 (중복 생성 방지)
    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    public enum FeeType { FREE, PAID, HOURLY }

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false)
    @Builder.Default
    private FeeType feeType = FeeType.FREE;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "fee_unit", length = 50)
    private String feeUnit;

    @Column(name = "operating_hours", length = 255)
    private String operatingHours;

    @Column(length = 255)
    private String location;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public enum Category { IN_ROOM, IN_HOTEL, LEISURE, FNB, BUSINESS, OTHER }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Category category = Category.OTHER;

    // 호텔-편의시설 연결 (양방향)
    @OneToMany(mappedBy = "amenity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HotelAmenity> hotelAmenities = new ArrayList<>();
}
