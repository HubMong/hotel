package com.example.backend.hotel_reservation.controller;

import com.example.backend.hotel_reservation.dto.ReservationDtos.*;
import com.example.backend.hotel_reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService service;

    @PostMapping("/hold")
    public HoldResponse hold(@RequestBody HoldRequest req) {
        return service.hold(req);
    }

    @PostMapping("/{id}/confirm")
    public void confirm(@PathVariable Long id) {
        service.confirm(id);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id) {
        service.cancel(id);
    }

    @GetMapping("/{id}")
    public ReservationDetail get(@PathVariable Long id) {
        return service.get(id);
    }
}
