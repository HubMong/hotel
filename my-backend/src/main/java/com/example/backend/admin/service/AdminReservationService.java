package com.example.backend.admin.service;

import com.example.backend.admin.dto.ReservationDetailDto;
import com.example.backend.admin.repository.AdminPaymentRepository;
import com.example.backend.admin.repository.AdminReservationRepository; // ★ 변경: 관리자용 리포
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReservationService {

    // ★ 변경: com.example.backend.admin.repository.AdminReservationRepository 사용
    private final AdminReservationRepository reservationRepository;
    private final AdminPaymentRepository paymentRepository;

    // ==== 유틸: LDT → Instant ====
    private Instant toInstant(LocalDateTime ldt) {
        return ldt == null ? null : ldt.atZone(ZoneId.systemDefault()).toInstant();
    }

    public Page<Reservation> list(ReservationStatus status,
                                  LocalDateTime from, LocalDateTime to,
                                  LocalDateTime stayFrom, LocalDateTime stayTo,
                                  Pageable pageable) {
        return reservationRepository.search(
                status,
                toInstant(from),
                toInstant(to),
                toInstant(stayFrom),
                toInstant(stayTo),
                pageable
        );
    }

    public Page<ReservationDetailDto> listWithDetails(ReservationStatus reservationStatus,
                                                      LocalDateTime from, LocalDateTime to,
                                                      LocalDateTime stayFrom, LocalDateTime stayTo,
                                                      String hotelName, String userName,
                                                      String paymentStatus,
                                                      Pageable pageable) {
        String statusStr = reservationStatus != null ? reservationStatus.name() : null;

        Long totalPayments = paymentRepository.countAllPayments();
        log.info("=== 결제 데이터 디버깅 === 전체 결제 수: {}", totalPayments);

        Page<Object[]> results = reservationRepository.searchWithDetails(
                statusStr, from, to, stayFrom, stayTo, hotelName, userName, paymentStatus, pageable);

        List<ReservationDetailDto> dtos = results.getContent().stream()
                .map(this::mapArrayToDto)
                .toList();

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    public Reservation get(Long id) {
        return reservationRepository.findById(id).orElseThrow();
    }

    public ReservationDetailDto getDetail(Long id) {
        log.info("예약 상세 조회 시작 - ID: {}", id);
        try {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("유효하지 않은 예약 ID: " + id);
            }
            if (!reservationRepository.existsById(id)) {
                throw new RuntimeException("예약을 찾을 수 없습니다: " + id);
            }

            Object[] queryResult = reservationRepository.findDetailById(id);
            if (queryResult == null) throw new RuntimeException("예약 상세 정보가 없습니다: " + id);

            Object[] row = (queryResult.length == 1 && queryResult[0] instanceof Object[])
                    ? (Object[]) queryResult[0] : queryResult;

            return mapArrayToDto(row);

        } catch (Exception e) {
            log.error("예약 상세 조회 오류 - ID: {}, err={}", id, e.getMessage(), e);
            throw (e instanceof RuntimeException re) ? re
                    : new RuntimeException("예약 상세 조회 중 오류: " + e.getMessage(), e);
        }
    }

    public void cancel(Long id) {
        Reservation r = reservationRepository.findById(id).orElseThrow();
        r.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(r);
    }

    // ===== 안전 변환 & 매핑 =====
    private ReservationDetailDto mapArrayToDto(Object[] row) {
        if (row == null || row.length < 9) {
            throw new IllegalArgumentException("예약 기본 정보가 불완전합니다.");
        }
        return ReservationDetailDto.builder()
                .reservationId(safeLong(row[0]))
                .transactionId(safeString(row[1]))
                .numAdult(safeInt(row[2]))
                .numKid(safeInt(row[3]))
                .startDate(safeDateTime(row[4]))
                .endDate(safeDateTime(row[5]))
                .createdAt(safeDateTime(row[6]))
                .reservationStatus(safeString(row[7]))
                .expiresAt(safeDateTime(row[8]))

                .hotelId(safeLong(row, 9))
                .hotelName(safeString(row, 10, "호텔 정보 없음"))
                .hotelAddress(safeString(row, 11, "주소 정보 없음"))
                .starRating(safeInt(row, 12, 0))

                .roomId(safeLong(row, 13))
                .roomName(safeString(row, 14, "객실 정보 없음"))
                .roomType(safeString(row, 15, "객실 타입 정보 없음"))
                .capacityMin(safeInt(row, 16, 0))
                .capacityMax(safeInt(row, 17, 0))
                .roomPrice(safeInt(row, 18, 0))

                .userId(safeLong(row, 19))
                .userName(safeString(row, 20, "사용자 정보 없음"))
                .userEmail(safeString(row, 21, "이메일 정보 없음"))
                .userPhone(safeString(row, 22, "전화번호 정보 없음"))

                .paymentId(safeLong(row, 23))
                .paymentMethod(safeString(row, 24, "결제 정보 없음"))
                .basePrice(safeInt(row, 25, 0))
                .totalPrice(safeInt(row, 26, 0))
                .tax(safeInt(row, 27, 0))
                .discount(safeInt(row, 28, 0))
                .paymentStatus(safeString(row, 29, "결제 정보 없음"))
                .paymentCreatedAt(safeDateTime(row, 30))
                .receiptUrl(safeString(row, 31, null))
                .build();
    }

    private Long safeLong(Object v) {
        try { return v == null ? null : (v instanceof Number n ? n.longValue() : Long.parseLong(v.toString())); }
        catch (Exception e) { return null; }
    }
    private Long safeLong(Object[] a, int i) { return i < a.length ? safeLong(a[i]) : null; }

    private Integer safeInt(Object v) {
        try { return v == null ? null : (v instanceof Number n ? n.intValue() : Integer.parseInt(v.toString())); }
        catch (Exception e) { return null; }
    }
    private Integer safeInt(Object[] a, int i, int def) {
        Integer x = i < a.length ? safeInt(a[i]) : null; return x != null ? x : def;
    }

    private String safeString(Object v) { return v == null ? null : v.toString().trim(); }
    private String safeString(Object[] a, int i, String def) {
        String s = i < a.length ? safeString(a[i]) : null; return (s == null || s.isBlank()) ? def : s;
    }

    private LocalDateTime safeDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (v instanceof LocalDateTime ldt) return ldt;
        if (v instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        if (v instanceof java.util.Date d2) return d2.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return null;
    }
    private LocalDateTime safeDateTime(Object[] a, int i) { return i < a.length ? safeDateTime(a[i]) : null; }
}
