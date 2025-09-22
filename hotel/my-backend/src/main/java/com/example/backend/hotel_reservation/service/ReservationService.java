package com.example.backend.hotel_reservation.service;

import com.example.backend.hotel_reservation.domain.*;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
import com.example.backend.hotel_reservation.dto.ReservationDtos.*;
import com.example.backend.hotel_reservation.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {
    private final RoomInventoryRepository invRepo;
    private final ReservationRepository resRepo;
    private final RoomRepository roomRepo;

    private static LocalDate parseYmd(String s) {
        return LocalDate.parse(s); // 'YYYY-MM-DD' 가정
    }

    private static Instant toStartOfDayUtc(LocalDate d) {
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    // [checkIn, checkOut) 날짜 리스트
    private static List<LocalDate> days(LocalDate startInclusive, LocalDate endExclusive) {
        List<LocalDate> list = new ArrayList<>();
        for (LocalDate d = startInclusive; d.isBefore(endExclusive); d = d.plusDays(1)) list.add(d);
        return list;
    }

    // 없으면 기본 5객실 생성 (데모용)
    private RoomInventory getOrCreateLocked(Long roomId, LocalDate date) {
        return invRepo.findWithLock(roomId, date)
                .orElseGet(() -> invRepo.save(RoomInventory.builder()
                        .roomId(roomId)
                        .date(date)
                        .totalQuantity(5)
                        .availableQuantity(5)
                        .build()));
    }

    @Transactional
    public HoldResponse hold(HoldRequest req) {
        if (req.getQty() == null || req.getQty() < 1) throw new IllegalArgumentException("qty must be >= 1");

        LocalDate ci = parseYmd(req.getCheckIn());
        LocalDate co = parseYmd(req.getCheckOut());
        if (!ci.isBefore(co)) throw new IllegalArgumentException("checkOut must be after checkIn");

        int qty = req.getQty();
        List<LocalDate> stay = days(ci, co);

        // 1) 모든 날짜 락 + 재고 확인
        List<RoomInventory> locked = new ArrayList<>();
        for (LocalDate d : stay) {
            RoomInventory ri = getOrCreateLocked(req.getRoomId(), d); // PESSIMISTIC_WRITE
            if (ri.getAvailableQuantity() < qty) {
                throw new IllegalStateException("재고부족: " + d);
            }
            locked.add(ri);
        }

        // 2) 차감
        locked.forEach(ri -> ri.setAvailableQuantity(ri.getAvailableQuantity() - qty));
        invRepo.saveAllAndFlush(locked);

        // 3) PENDING 예약(홀드) 생성 + 만료시간
        int holdSec = Optional.ofNullable(req.getHoldSeconds()).orElse(30);
        Instant now = Instant.now();
        Reservation r = Reservation.builder()
                .userId(req.getUserId())
                .roomId(req.getRoomId())
                .numRooms(qty)
                .numAdult(Optional.ofNullable(req.getAdults()).orElse(0))
                .numKid(Optional.ofNullable(req.getChildren()).orElse(0))
                .startDate(toStartOfDayUtc(ci))
                .endDate(toStartOfDayUtc(co))
                .status(ReservationStatus.PENDING)
                .expiresAt(now.plusSeconds(holdSec))
                .build();
        resRepo.save(r);

        log.info("[HOLD] reservationId={} room={} dates={}~{} qty={} expiresAt={}",
                r.getId(), r.getRoomId(), ci, co.minusDays(1), qty, r.getExpiresAt());

        return HoldResponse.builder()
                .reservationId(r.getId())
                .expiresAt(r.getExpiresAt())
                .status(r.getStatus().name())
                .build();
    }

    @Transactional
    public void confirm(Long reservationId) {
        Reservation r = resRepo.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("예약 없음"));

        if (r.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("이미 처리됨: " + r.getStatus());
        }
        if (r.getExpiresAt() != null && Instant.now().isAfter(r.getExpiresAt())) {
            // 시간초과 → 재고 복구 후 CANCELLED
            cancelInternal(r);
            throw new IllegalStateException("시간초과로 예약이 만료되었습니다.");
        }
        r.setStatus(ReservationStatus.COMPLETED);
        resRepo.save(r);
        log.info("[CONFIRM] reservationId={} COMPLETED", r.getId());
    }

    @Transactional
    public void cancel(Long reservationId) {
        Reservation r = resRepo.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("예약 없음"));
        cancelInternal(r);
        log.info("[CANCEL] reservationId={} CANCELLED", r.getId());
    }

    // PENDING 이면 재고 복구
    private void cancelInternal(Reservation r) {
        if (r.getStatus() != ReservationStatus.PENDING) {
            r.setStatus(ReservationStatus.CANCELLED);
            resRepo.save(r);
            return;
        }
        LocalDate ci = r.getStartDate().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate co = r.getEndDate().atZone(ZoneOffset.UTC).toLocalDate();
        List<LocalDate> stay = days(ci, co);

        int qty = Optional.ofNullable(r.getNumRooms()).orElse(1);
        for (LocalDate d : stay) {
            RoomInventory ri = getOrCreateLocked(r.getRoomId(), d); // 락 후 복구
            ri.setAvailableQuantity(ri.getAvailableQuantity() + qty);
            invRepo.save(ri);
        }
        r.setStatus(ReservationStatus.CANCELLED);
        resRepo.save(r);
    }

    @Transactional(readOnly = true)
    public ReservationDtos.ReservationDetail get(Long id) {
        Reservation r = resRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("예약 없음"));
                Long hotelId = roomRepo.findHotelIdByRoomId(r.getRoomId());
        return ReservationDtos.ReservationDetail.builder()
                .id(r.getId())
                .status(r.getStatus().name())
                .expiresAt(r.getExpiresAt())
                .userId(r.getUserId())
                .roomId(r.getRoomId())
                .hotelId(hotelId)
                .numRooms(r.getNumRooms())
                .adults(r.getNumAdult())
                .children(r.getNumKid())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .build();
    }
}
