package com.example.backend.hotel_wishlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.hotel_wishlist.domain.Wishlist;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // ✅ 해당 유저가 특정 호텔을 찜했는지 여부 확인
    boolean existsByUserIdAndHotelId(Long userId, Long hotelId);

    // ✅ 유저의 위시리스트 전체 조회
    List<Wishlist> findByUserId(Long userId);

    // ✅ 유저가 특정 호텔을 찜한 내역 조회 (삭제 시 사용)
    Optional<Wishlist> findByUserIdAndHotelId(Long userId, Long hotelId);
}
