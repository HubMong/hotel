package com.example.backend;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addWishlist(@RequestBody WishlistDto dto, Authentication authentication) {
        // 토큰에서 username 꺼내오기
        String username = authentication.getName();

        // DB에서 userId 찾기
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 호텔 조회
        Hotel hotel = hotelRepository.findById(dto.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found"));

        boolean exists = wishlistRepository.existsByUserAndHotel(user, hotel);
        if (exists) {
            return ResponseEntity.badRequest().body("이미 찜한 호텔입니다.");
        }

        // Wishlist 저장
        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setHotel(hotel);

        return ResponseEntity.ok(wishlistRepository.save(wishlist));
    }

    @GetMapping("/me")
    public List<WishlistResponseDTO> getMyWishlist(Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Wishlist> wishlists = wishlistRepository.findByUser(user);

        return wishlists.stream()
                .map(w -> new WishlistResponseDTO(
                        w.getId(),
                        w.getHotel().getId(),
                        w.getHotel().getName(),
                        w.getHotel().getImageUrl()
                ))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<?> removeWishlist(@PathVariable Long wishlistId, Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new RuntimeException("Wishlist not found"));

        // 소유자 확인
        if (!wishlist.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        wishlistRepository.delete(wishlist);
        return ResponseEntity.ok().build();
    }

}
