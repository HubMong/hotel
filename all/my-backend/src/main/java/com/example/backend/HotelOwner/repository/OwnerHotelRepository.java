package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.authlogin.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerHotelRepository extends JpaRepository<Hotel, Long> {

    Optional<Hotel> findByOwner(User owner);

    //추가 문의 목록1010
    List<Hotel> findAllByIdIn(List<Long> ids);
}