package com.example.backend.HotelOwner.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = "hotel")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소속 호텔 (오너/FE 공용)
    @ManyToOne(fetch = FetchType.EAGER) // 기존 코드 유지(EAGER)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    // ★ FE에서 쓰던 name 필드 포함
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 50)
    private RoomType roomType;

    // ★ 오너에서 쓰던 타입도 유지
    public enum RoomType {디럭스룸, 스위트룸, 스탠다드룸, 싱글룸, 트윈룸}

    @Column(name = "room_size", length = 50)
    private String roomSize;

    @Column(name = "capacity_min", nullable = false)
    private Integer capacityMin;

    @Column(name = "capacity_max", nullable = false)
    private Integer capacityMax;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime;

    @Column(name = "check_out_time", nullable = false)
    private LocalTime checkOutTime;

    // FE 확장 필드들(옵션)
    @Column(name = "view_name", length = 50)
    private String viewName;

    @Column(name = "bed", length = 50)
    private String bed;

    @Column(name = "bath")
    private Integer bath;

    @Column(name = "smoke")
    private Boolean smoke;

    @Column(name = "shared_bath")
    private Boolean sharedBath;

    @Column(name = "has_window")
    private Boolean hasWindow;

    @Column(name = "aircon")
    private Boolean aircon;

    @Column(name = "free_water")
    private Boolean freeWater;

    @Column(name = "wifi")
    private Boolean wifi;

    @Column(name = "cancel_policy", length = 100)
    private String cancelPolicy;

    @Column(name = "payment", length = 50)
    private String payment;

    // 가격(원가/판매가)
    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "price", nullable = false)
    private Integer price; // 오너쪽 int → Integer로 통일(Null 허용시 마이그레이션 편함)

    // 같은 타입 방 개수 (오너 필드 유지)
    @Column(name = "room_count", nullable = false)
    private Integer roomCount;

    // 객실 상태 (available, occupied, maintenance, cleaning)
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "available";

    // 이미지
    @Builder.Default
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RoomImage> images = new ArrayList<>();
}
