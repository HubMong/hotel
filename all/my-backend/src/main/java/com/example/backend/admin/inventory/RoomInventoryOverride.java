package com.example.backend.admin.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_inventory_override", indexes = {
        @Index(name = "idx_room_date", columnList = "room_id,date")
})
@Getter
@Setter
@NoArgsConstructor
public class RoomInventoryOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    // open|closed|maintenance|cleaning  (open은 강제 오픈 의미, closed는 차단)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
