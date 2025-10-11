package com.example.backend.hotel_wishlist.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.hotel_wishlist.dto.WishlistDto;
import com.example.backend.hotel_wishlist.dto.WishlistRequest;
import com.example.backend.hotel_wishlist.service.WishlistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final LoginRepository loginRepository;

    private String extractEmail(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        return (principal instanceof String s) ? s : null;
    }

    /* ========================================================
       🔹 찜 추가
       ======================================================== */
    @PostMapping
    public ResponseEntity<?> addToWishlist(Authentication authentication,
                                           @RequestBody WishlistRequest request) {
        String email = extractEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        User user = loginRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        wishlistService.addToWishlist(user.getId(), request.getHotelId());
        return ResponseEntity.ok(Map.of("message", "찜 추가 완료"));
    }

    /* ========================================================
       🔹 찜 삭제
       ======================================================== */
    @DeleteMapping("/{hotelId}")
    public ResponseEntity<?> removeWishlist(Authentication authentication,
                                            @PathVariable Long hotelId) {
        String email = extractEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        User user = loginRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        wishlistService.removeWishlist(user.getId(), hotelId);
        return ResponseEntity.ok(Map.of("message", "찜 취소 완료"));
    }

    /* ========================================================
       🔹 내 위시리스트 조회
       ======================================================== */
    @GetMapping
    public ResponseEntity<?> getMyWishlist(Authentication authentication) {
        String email = extractEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        User user = loginRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<WishlistDto> list = wishlistService.getMyWishlist(user.getId());
        return ResponseEntity.ok(list);
    }
}

