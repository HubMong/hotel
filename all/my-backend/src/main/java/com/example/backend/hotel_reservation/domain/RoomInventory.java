package com.example.backend.hotel_reservation.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "Room_Inventory",
       uniqueConstraints = @UniqueConstraint(name="uq_room_day", columnNames={"room_id", "date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomInventory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="room_id", nullable=false)
    private Long roomId;

    @Column(name="date", nullable=false)
    private LocalDate date;

    @Column(name="total_quantity", nullable=false)
    private Integer totalQuantity;

    @Column(name="available_quantity", nullable=false)
    private Integer availableQuantity;
}
