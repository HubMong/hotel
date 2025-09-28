package com.example.backend.hotel_reservation.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "Room_Price_Policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomPricePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", nullable = false)
    private SeasonType seasonType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "price", nullable = false)
    private Integer price;

    // --- ENUMs ---
    public enum SeasonType { PEAK, OFF_PEAK, HOLIDAY }
    public enum DayType { WEEKDAY, FRI, SAT, SUN }
}