package com.example.backend.hotel_wishlist.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelImage;
import com.example.backend.HotelOwner.repository.HotelImageRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.hotel_wishlist.domain.Wishlist;
import com.example.backend.hotel_wishlist.dto.WishlistDto;
import com.example.backend.hotel_wishlist.repository.WishlistRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository; // ✅ 사용
    private final RoomRepository roomRepository;             // ✅ 사용

    /* 찜 추가 */
    @Transactional
    public void addToWishlist(Long userId, Long hotelId) {
        if (wishlistRepository.existsByUserIdAndHotelId(userId, hotelId)) {
            throw new IllegalStateException("이미 찜한 호텔입니다.");
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다."));

        Wishlist wishlist = Wishlist.builder()
                .userId(userId)
                .hotel(hotel)
                .build();

        wishlistRepository.save(wishlist);
    }

    /* 찜 삭제 */
    @Transactional
    public void removeWishlist(Long userId, Long hotelId) {
        wishlistRepository.findByUserIdAndHotelId(userId, hotelId)
                .ifPresent(wishlistRepository::delete);
    }

    /* 내 위시리스트 조회 */
    @Transactional(readOnly = true)
    public List<WishlistDto> getMyWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(w -> {
                    Long hotelId = w.getHotel().getId();

                    // 대표 이미지 (없으면 기본 이미지)
                    String imageUrl = hotelImageRepository.findByHotel_IdOrderBySortNoAsc(hotelId).stream()
                            .filter(HotelImage::isCover)
                            .map(HotelImage::getUrl)
                            .findFirst()
                            .orElse("/default.png");

                    // 객실 최저가 (없으면 0)
                    Integer lowestPrice = roomRepository.findByHotelId(hotelId).stream()
                            .map(r -> r.getPrice())
                            .filter(p -> p != null)
                            .min(Integer::compareTo)
                            .orElse(0);

                    return WishlistDto.builder()
                            .wishlistId(w.getId())
                            .hotelId(hotelId)
                            .hotelName(w.getHotel().getName())
                            .hotelAddress(w.getHotel().getAddress())
                            .hotelImageUrl(imageUrl)
                            .hotelPrice(lowestPrice)
                            .build();
                })
                .toList();
    }
}
