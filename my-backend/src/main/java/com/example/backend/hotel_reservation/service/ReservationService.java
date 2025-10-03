package com.example.backend.hotel_reservation.service;

import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.RoomInventory;
import com.example.backend.hotel_reservation.dto.ReservationDtos;
import com.example.backend.hotel_reservation.dto.ReservationDtos.HoldRequest;
import com.example.backend.hotel_reservation.dto.ReservationDtos.HoldResponse;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.hotel_reservation.repository.RoomInventoryRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        return LocalDate.parse(s);
    }

    private static Instant toStartOfDayUtc(LocalDate d) {
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static List<LocalDate> days(LocalDate startInclusive, LocalDate endExclusive) {
        List<LocalDate> list = new ArrayList<>();
        for (LocalDate d = startInclusive; d.isBefore(endExclusive); d = d.plusDays(1)) list.add(d);
        return list;
    }

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
        if (req.getUserId() == null) throw new IllegalArgumentException("userId is required (from JWT)");
        if (req.getQty() == null || req.getQty() < 1) throw new IllegalArgumentException("qty must be >= 1");

        LocalDate ci = parseYmd(req.getCheckIn());
        LocalDate co = parseYmd(req.getCheckOut());
        if (!ci.isBefore(co)) throw new IllegalArgumentException("checkOut must be after checkIn");

        int qty = req.getQty();
        List<LocalDate> stay = days(ci, co);

        List<RoomInventory> locked = new ArrayList<>();
        for (LocalDate d : stay) {
            RoomInventory ri = getOrCreateLocked(req.getRoomId(), d);
            if (ri.getAvailableQuantity() < qty) {
                throw new IllegalStateException("재고부족: " + d);
            }
            locked.add(ri);
        }

        locked.forEach(ri -> ri.setAvailableQuantity(ri.getAvailableQuantity() - qty));
        invRepo.saveAllAndFlush(locked);

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
                .status(Reservation.Status.PENDING)
                .expiresAt(now.plusSeconds(holdSec))
                .build();
        resRepo.save(r);

        log.info("[HOLD] userId={} reservationId={} room={} {}~{} qty={} expiresAt={}",
                r.getUserId(), r.getId(), r.getRoomId(), ci, co.minusDays(1), qty, r.getExpiresAt());

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

        if (r.getStatus() == Reservation.Status.COMPLETED) {
            log.info("[CONFIRM] reservationId={} 이미 COMPLETED (idempotent)", r.getId());
            return;
        }
    if (r.getStatus() == Reservation.Status.CANCELLED) {
            throw new IllegalStateException("취소된 예약은 확정 불가");
        }
        if (r.getExpiresAt() != null && Instant.now().isAfter(r.getExpiresAt())) {
            cancelInternal(r);
            throw new IllegalStateException("시간초과로 예약이 만료되었습니다.");
        }
    r.setStatus(Reservation.Status.COMPLETED);
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

    // ★ 오너가 자기 호텔 예약을 취소
    @Transactional
    public void cancelByOwner(Long reservationId, Long ownerId) {
        Reservation r = resRepo.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException("예약 없음"));

        boolean allowed = resRepo.isOwnedBy(reservationId, ownerId);
        if (!allowed) {
            throw new SecurityException("본인 소유 호텔의 예약만 취소할 수 있습니다.");
        }

        cancelInternal(r);
        log.info("[OWNER-CANCEL] ownerId={} reservationId={} -> CANCELLED", ownerId, reservationId);
    }

    private void cancelInternal(Reservation r) {
        if (r.getStatus() != Reservation.Status.PENDING) {
            r.setStatus(Reservation.Status.CANCELLED);
            resRepo.save(r);
            return;
        }
        LocalDate ci = r.getStartDate().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate co = r.getEndDate().atZone(ZoneOffset.UTC).toLocalDate();
        List<LocalDate> stay = days(ci, co);

        int qty = Optional.ofNullable(r.getNumRooms()).orElse(1);
        for (LocalDate d : stay) {
            RoomInventory ri = getOrCreateLocked(r.getRoomId(), d);
            ri.setAvailableQuantity(ri.getAvailableQuantity() + qty);
            invRepo.save(ri);
        }
    r.setStatus(Reservation.Status.CANCELLED);
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

    @Transactional(readOnly = true)
    public List<ReservationDtos.ReservationSummary> getByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        var pageRes = resRepo.findByUserId(userId, pageable);

        List<ReservationDtos.ReservationSummary> list = new ArrayList<>(pageRes.getNumberOfElements());
        for (Reservation r : pageRes.getContent()) {
            Long hotelId = roomRepo.findHotelIdByRoomId(r.getRoomId());
            list.add(ReservationDtos.ReservationSummary.builder()
                    .id(r.getId())
                    .status(r.getStatus().name())
                    .userId(r.getUserId())
                    .roomId(r.getRoomId())
                    .hotelId(hotelId)
                    .numRooms(r.getNumRooms())
                    .adults(r.getNumAdult())
                    .children(r.getNumKid())
                    .startDate(r.getStartDate())
                    .endDate(r.getEndDate())
                    .build());
        }
        log.info("[MY] userId={} -> {} rows", userId, list.size());
        return list;
    }

    /**
     * 예약을 강제로 만료시킵니다 (PENDING -> CANCELLED)
     * 재고를 복구합니다.
     */
    @Transactional
    public void expire(Long id) {
        Reservation r = resRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("예약 없음: " + id));

        // 이미 CANCELLED면 멱등 처리
        if (r.getStatus() == Reservation.Status.CANCELLED) {
            log.info("[EXPIRE] reservationId={} 이미 CANCELLED (idempotent)", id);
            return;
        }

        // COMPLETED는 만료 불가
        if (r.getStatus() == Reservation.Status.COMPLETED) {
            throw new IllegalStateException("완료된 예약은 만료할 수 없습니다: " + id);
        }

        // PENDING 상태만 만료 처리
        if (r.getStatus() == Reservation.Status.PENDING) {
            // 재고 복구
            LocalDate ci = r.getStartDate().atZone(ZoneOffset.UTC).toLocalDate();
            LocalDate co = r.getEndDate().atZone(ZoneOffset.UTC).toLocalDate();
            int qty = Optional.ofNullable(r.getNumRooms()).orElse(1);

            for (LocalDate d : days(ci, co)) {
                RoomInventory inv = getOrCreateLocked(r.getRoomId(), d);
                inv.setAvailableQuantity(inv.getAvailableQuantity() + qty);
                invRepo.save(inv);
            }
        }

        r.setStatus(Reservation.Status.CANCELLED);
        resRepo.save(r);
        log.info("[EXPIRE] reservationId={} → CANCELLED (재고 복구 완료)", id);
    }
}
