package com.example.backend.mypage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.mypage.dto.PasswordChangeDto;
import com.example.backend.mypage.dto.UserUpdateDto;
import com.example.backend.mypage.service.UserService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    // ... 기존 다른 메소드들 ...

    // ▼▼▼ [수정] 경로에 {id} 추가, @PathVariable 추가 ▼▼▼
    @PatchMapping("/api/users/{id}") 
    public ResponseEntity<Void> updateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserUpdateDto userUpdateDto) {
        
        userService.updateEmail(id, userDetails.getUsername(), userUpdateDto.getEmail());
        
        return ResponseEntity.ok().build();
    }
    
    // ▼▼▼ [수정] 경로에 {id} 추가, @PathVariable 추가 ▼▼▼
    @PatchMapping("/api/users/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PasswordChangeDto passwordChangeDto) {
        
        userService.changePassword(id, userDetails.getUsername(), 
                                   passwordChangeDto.getCurrentPassword(), 
                                   passwordChangeDto.getNewPassword());
        
        return ResponseEntity.ok().build();
    }
}