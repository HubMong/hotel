package com.example.backend;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/hotel")
@RequiredArgsConstructor
public class HotelController {

    private final HotelRepository repository;

    @PostMapping("/add")
    public String add(@RequestBody Hotel hotel) {

        Hotel inputHotel = new Hotel();
        inputHotel.setId(hotel.getId());
        inputHotel.setName(hotel.getName());

        repository.save(inputHotel);

        return "호텔 추가 완료";
    }

    @GetMapping("/lists")
    public List<Hotel> lists() {
        List<Hotel> result = new ArrayList<Hotel>();
        result = repository.findAll();

        return result;
    }

}
