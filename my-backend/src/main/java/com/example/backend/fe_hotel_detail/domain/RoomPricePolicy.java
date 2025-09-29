package com.example.backend.fe_hotel_detail.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Room_Price_Policy")
@Getter
@Setter
@NoArgsConstructor
public class RoomPricePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    public enum SeasonType { PEAK, OFF_PEAK, HOLIDAY }
    public enum DayType { WEEKDAY, FRI, SAT, SUN }

    @Enumerated(EnumType.STRING)
    @Column(name = "season_type", nullable = false)
    private SeasonType seasonType = SeasonType.OFF_PEAK;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", nullable = false)
    private DayType dayType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer price;
}
