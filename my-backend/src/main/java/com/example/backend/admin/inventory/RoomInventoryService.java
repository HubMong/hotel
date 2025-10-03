package com.example.backend.admin.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomInventoryService {
    private final RoomInventoryOverrideRepository repo;

    public List<RoomInventoryOverride> listForRoomsAndMonth(List<Long> roomIds, LocalDate from, LocalDate to) {
        if (roomIds == null || roomIds.isEmpty()) return List.of();
        return repo.findByRoomIdInAndDateBetween(roomIds, from, to);
    }

    public List<RoomInventoryOverride> createOverrides(Long roomId, LocalDate from, LocalDate to, String status, String note) {
        String s = normalize(status);
        List<RoomInventoryOverride> created = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            RoomInventoryOverride ov = new RoomInventoryOverride();
            ov.setRoomId(roomId);
            ov.setDate(cursor);
            ov.setStatus(s);
            ov.setNote(note);
            created.add(repo.save(ov));
            cursor = cursor.plusDays(1);
        }
        return created;
    }

    public void deleteOverride(Long id) { repo.deleteById(id); }

    private String normalize(String raw) {
        if (raw == null) return "closed";
        switch (raw.trim().toLowerCase()) {
            case "open":
                return "open";
            case "maintenance":
            case "점검":
            case "점검중":
                return "maintenance";
            case "cleaning":
            case "청소":
            case "청소중":
                return "cleaning";
            case "closed":
            case "차단":
            default:
                return "closed";
        }
    }

    public List<RoomInventoryOverride> upsertOverrides(Long roomId, LocalDate from, LocalDate to, String status, String note) {
        String s = normalize(status);
        List<RoomInventoryOverride> results = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            results.add(upsertOverride(roomId, cursor, s, note));
            cursor = cursor.plusDays(1);
        }
        return results;
    }

    public RoomInventoryOverride upsertOverride(Long roomId, LocalDate date, String status, String note) {
        String s = normalize(status);
        RoomInventoryOverride ov = repo.findByRoomIdAndDate(roomId, date)
                .orElseGet(RoomInventoryOverride::new);
        ov.setRoomId(roomId);
        ov.setDate(date);
        ov.setStatus(s);
        ov.setNote(note);
        return repo.save(ov);
    }
}
