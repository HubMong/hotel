package com.example.backend.hotel_reservation.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Room")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(nullable = false, length = 100)
    private String name;

    // ✅ DB: room_size (varchar)
    @Column(name = "room_size", length = 50)
    private String roomSize;

    @Column(name = "capacity_min")
    private Integer capacityMin;

    @Column(name = "capacity_max")
    private Integer capacityMax;

    // ===== 추가 컬럼들 (하이버네이트 로그에 등장한 필드들) =====
    @Column(name = "view_name", length = 50)
    private String viewName;

    @Column(length = 50)
    private String bed;

    @Column
    private Integer bath;

    @Column
    private Boolean smoke;

    @Column(name = "shared_bath")
    private Boolean sharedBath;

    @Column(name = "has_window")
    private Boolean hasWindow;

    @Column
    private Boolean aircon;

    @Column(name = "free_water")
    private Boolean freeWater;

    @Column
    private Boolean wifi;

    @Column(name = "cancel_policy", length = 100)
    private String cancelPolicy;

    @Column(length = 50)
    private String payment;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column
    private Integer price;

    // 스키마에는 있지만 화면에서 안 쓰는 시간 필드(있으면 매핑/없으면 주석)
    @Column(name = "check_in_time")
    private java.sql.Time checkInTime;

    @Column(name = "check_out_time")
    private java.sql.Time checkOutTime;
}
