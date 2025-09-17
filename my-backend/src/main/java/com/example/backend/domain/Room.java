// src/main/java/com/example/backend/domain/Room.java

package com.example.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "room") // ✅ DB 테이블명과 동일하게 수정
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @Column(name = "room_size")   // ✅ DB 컬럼명 명시
    private String roomSize;

    @Column(name = "capacity_min") // ✅ DB 컬럼명 명시
    private int capacityMin;

    @Column(name = "capacity_max") // ✅ DB 컬럼명 명시
    private int capacityMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id") // ✅ Hotel 엔티티와 FK 매핑
    private Hotel hotel;

    // 기본 생성자
    public Room() {}

    // Getter 및 Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRoomSize() { return roomSize; }
    public void setRoomSize(String roomSize) { this.roomSize = roomSize; }

    public int getCapacityMin() { return capacityMin; }
    public void setCapacityMin(int capacityMin) { this.capacityMin = capacityMin; }

    public int getCapacityMax() { return capacityMax; }
    public void setCapacityMax(int capacityMax) { this.capacityMax = capacityMax; }

    public Hotel getHotel() { return hotel; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
}
