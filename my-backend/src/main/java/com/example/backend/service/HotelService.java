package com.example.backend.service;

import com.example.backend.domain.Hotel;
import com.example.backend.repository.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HotelService {

    @Autowired
    private HotelRepository hotelRepository;

    public List<Hotel> searchHotels(String destination, Integer guests) {
        // Repository의 커스텀 쿼리 메소드를 호출
        return hotelRepository.findHotelsWithFilter(destination, guests);
    }
}