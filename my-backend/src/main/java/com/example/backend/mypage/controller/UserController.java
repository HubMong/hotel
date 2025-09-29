package com.example.backend.mypage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; 
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.mypage.dto.PasswordChangeDto;
import com.example.backend.mypage.dto.UserUpdateDto;
import com.example.backend.mypage.service.UserService;
import com.example.backend.mypage.dto.ProfileUpdateAndPassDto; // 💡 [추가] 단일 수정 DTO

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus; 
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    // ▼▼▼ [기존] 이메일 개별 수정 API 유지 (하위 호환성) ▼▼▼
    @PatchMapping("/api/users/{id}") 
    public ResponseEntity<Void> updateUser(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody UserUpdateDto userUpdateDto) {
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 누락되었습니다.");
        }
        
        String username = authentication.getPrincipal().toString();
        
        userService.updateEmail(id, username, userUpdateDto.getEmail());
        
        return ResponseEntity.ok().build();
    }
    
    // ▼▼▼ [기존] 비밀번호 개별 수정 API 유지 (하위 호환성) ▼▼▼
    @PatchMapping("/api/users/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody PasswordChangeDto passwordChangeDto) {

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 누락되었습니다.");
        }
        
        String username = authentication.getPrincipal().toString();
        
        userService.changePassword(id, username, 
                                       passwordChangeDto.getCurrentPassword(), 
                                       passwordChangeDto.getNewPassword());
        
        return ResponseEntity.ok().build();
    }
    
    // 🏆 [핵심 추가] 이메일/비밀번호 동시 수정을 위한 단일 API (문제 해결용)
    @PatchMapping("/api/users/{id}/profile") 
    public ResponseEntity<Void> updateProfileAndPass(
            @PathVariable Long id,
            Authentication authentication, 
            @RequestBody ProfileUpdateAndPassDto dto) { // 💡 단일 DTO 사용
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 정보가 누락되었습니다.");
        }
        
        String username = authentication.getPrincipal().toString();
        
        // 💡 단일 서비스 메서드를 호출하여 트랜잭션으로 처리
        userService.updateProfileAndPass(
            id, 
            username, 
            dto.getEmail(), 
            dto.getCurrentPassword(), 
            dto.getNewPassword()
        );
        
        return ResponseEntity.ok().build();
    }
}