package com.example.backend.admin.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Value
@Builder
public class ReservationCalendarDayDto {
    LocalDate date;
    int totalReservations;
    int confirmedReservations;
    int pendingReservations;
    int cancelledReservations;
    List<ReservationSummary> reservations;

    @Value
    @Builder
    public static class ReservationSummary {
        Long reservationId;
        String guestName;
        int guestCount;
        String status;
        String roomType;

        static ReservationSummary fromJson(ObjectMapper mapper, java.util.Map<String, Object> map) {
            try {
                Long reservationId = null;
                if (map.containsKey("reservationId") && map.get("reservationId") != null) {
                    Object idObj = map.get("reservationId");
                    if (idObj instanceof Number) {
                        reservationId = ((Number) idObj).longValue();
                    } else if (idObj instanceof String) {
                        reservationId = Long.parseLong((String) idObj);
                    }
                }
                
                Integer guestCount = 0;
                if (map.containsKey("guestCount") && map.get("guestCount") != null) {
                    Object countObj = map.get("guestCount");
                    if (countObj instanceof Number) {
                        guestCount = ((Number) countObj).intValue();
                    } else if (countObj instanceof String) {
                        guestCount = Integer.parseInt((String) countObj);
                    }
                }
                
                return ReservationSummary.builder()
                        .reservationId(reservationId)
                        .guestName((String) map.getOrDefault("guestName", ""))
                        .guestCount(guestCount)
                        .status((String) map.getOrDefault("status", ""))
                        .roomType((String) map.getOrDefault("roomType", ""))
                        .build();
            } catch (Exception e) {
                System.err.println("Error parsing reservation summary: " + e.getMessage());
                return ReservationSummary.builder()
                        .reservationId(null)
                        .guestName("")
                        .guestCount(0)
                        .status("")
                        .roomType("")
                        .build();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static ReservationCalendarDayDto from(Object[] row) {
        ObjectMapper mapper = new ObjectMapper();
        List<ReservationSummary> reservationSummaries = Collections.emptyList();
        try {
            if (row.length > 5 && row[5] != null && !row[5].toString().trim().isEmpty()) {
                String jsonStr = row[5].toString().trim();
                // Handle case where GROUP_CONCAT might return empty or invalid JSON
                if (!jsonStr.equals("[]") && !jsonStr.equals("[null]")) {
                    List<java.util.Map<String, Object>> list = mapper.readValue(jsonStr, List.class);
                    reservationSummaries = list.stream()
                            .filter(entry -> entry != null && entry.containsKey("reservationId"))
                            .map(entry -> ReservationSummary.fromJson(mapper, entry))
                            .toList();
                }
            }
        } catch (JsonProcessingException e) {
            System.err.println("Failed to parse reservation JSON: " + e.getMessage());
            reservationSummaries = Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Unexpected error parsing reservations: " + e.getMessage());
            reservationSummaries = Collections.emptyList();
        }

        LocalDate date;
        Object d = row[0];
        if (d instanceof java.sql.Date sqlDate) {
            date = sqlDate.toLocalDate();
        } else if (d instanceof java.time.LocalDate ld) {
            date = ld;
        } else if (d instanceof java.sql.Timestamp ts) {
            date = ts.toLocalDateTime().toLocalDate();
        } else if (d instanceof java.util.Date ud) {
            date = new java.sql.Date(ud.getTime()).toLocalDate();
        } else {
            date = LocalDate.parse(d.toString());
        }

        return ReservationCalendarDayDto.builder()
                .date(date)
                .totalReservations(row.length > 1 && row[1] != null ? ((Number) row[1]).intValue() : 0)
                .confirmedReservations(row.length > 2 && row[2] != null ? ((Number) row[2]).intValue() : 0)
                .pendingReservations(row.length > 3 && row[3] != null ? ((Number) row[3]).intValue() : 0)
                .cancelledReservations(row.length > 4 && row[4] != null ? ((Number) row[4]).intValue() : 0)
                .reservations(reservationSummaries)
                .build();
    }
}

