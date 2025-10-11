package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.dto.OwnerDashboardDto;
import com.example.backend.HotelOwner.dto.OwnerSalesGraphDataDto;
import com.example.backend.HotelOwner.repository.OwnerPaymentRepository; // 수정
import com.example.backend.HotelOwner.repository.OwnerReservationRepository; // 수정
import com.example.backend.hotel_reservation.domain.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OwnerDashboardService {

    private final OwnerPaymentRepository ownerPaymentRepository;
    private final OwnerReservationRepository ownerReservationRepository; 

    public OwnerDashboardDto getDashboardSummary(Long ownerId, Long hotelId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime weekStart = todayStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime monthStart = todayStart.withDayOfMonth(1);

        long todaySales = ownerPaymentRepository.getSalesFromByHotelId(todayStart, hotelId);
        long weeklySales = ownerPaymentRepository.getSalesFromByHotelId(weekStart, hotelId);
        long monthlySales = ownerPaymentRepository.getSalesFromByHotelId(monthStart, hotelId);


        return OwnerDashboardDto.builder()
                .todaySales(todaySales)
                .weeklySales(weeklySales)
                .monthlySales(monthlySales)
                .build();
    }

    public OwnerSalesGraphDataDto getSalesGraphData(Long ownerId, Long hotelId, String roomType, String period, String analysisType, LocalDate startDate, LocalDate endDate) {

        LocalDateTime end = (endDate != null) ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        LocalDateTime start;
        String format;

        if ("daily".equals(analysisType)) {
            format = "%Y-%m-%d";
            if ("7d".equals(period)) {
                start = end.minusDays(6).toLocalDate().atStartOfDay();
            } else if ("30d".equals(period)) {
                start = end.minusDays(29).toLocalDate().atStartOfDay();
            } else {
                start = (startDate != null) ? startDate.atStartOfDay() : end.minusDays(6).toLocalDate().atStartOfDay();
            }
        } else if ("monthly".equals(analysisType)) {
            format = "%Y-%m";
            if ("1y".equals(period)) {
                start = end.minusYears(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
            } else {
                start = (startDate != null) ? startDate.withDayOfMonth(1).atStartOfDay() : end.minusYears(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
            }
        } else { // yearly
            format = "%Y";
            start = (startDate != null) ? startDate.withDayOfYear(1).atStartOfDay() : end.minusYears(4).withDayOfYear(1).toLocalDate().atStartOfDay();
        }

        List<Object[]> results = ownerPaymentRepository.findSalesByPeriodAndHotelId(start, end, roomType, format, hotelId);
        List<OwnerSalesGraphDataDto.DataPoint> dataPoints = results.stream()
                .map(row -> OwnerSalesGraphDataDto.DataPoint.builder()
                        .label((String) row[0])
                        .value(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        return OwnerSalesGraphDataDto.builder().dataPoints(dataPoints).build();
    }
}