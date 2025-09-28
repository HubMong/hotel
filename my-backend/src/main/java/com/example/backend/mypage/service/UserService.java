package com.example.backend.mypage.service;

import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.authlogin.domain.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder; 
    
    // ▼▼▼ [수정] 첫 번째 파라미터로 Long id를 받도록 변경 ▼▼▼
    public void updateEmail(Long id, String username, String newEmail) {
        // 1. 현재 사용자(토큰 기준) 정보를 DB에서 조회
        User userFromToken = loginRepository.findByEmail(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // ▼▼▼ [추가] '본인 확인' 보안 로직 ▼▼▼
        // 토큰의 주인 ID와 경로의 ID가 일치하는지 확인합니다.
        // 이를 통해 다른 사용자가 URL의 숫자만 바꿔서 정보를 수정하는 것을 방지합니다.
        if (!userFromToken.getId().equals(id)) {
            throw new AccessDeniedException("자신의 정보만 수정할 수 있습니다.");
        }

        // (선택) 2. 새로운 이메일이 이미 사용 중인지 확인
        if (loginRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 3. 이메일 변경 후 저장
        userFromToken.setEmail(newEmail);
        loginRepository.save(userFromToken);
    }
    
    // ▼▼▼ [수정] 첫 번째 파라미터로 Long id를 받도록 변경 ▼▼▼
    public void changePassword(Long id, String username, String currentPassword, String newPassword) {
        // 1. 사용자 조회 (토큰 기준)
        User userFromToken = loginRepository.findByEmail(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // ▼▼▼ [추가] '본인 확인' 보안 로직 ▼▼▼
        if (!userFromToken.getId().equals(id)) {
            throw new AccessDeniedException("자신의 정보만 수정할 수 있습니다.");
        }

        // 2. 현재 비밀번호가 일치하는지 확인
        if (!passwordEncoder.matches(currentPassword, userFromToken.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 3. 새 비밀번호를 암호화하여 저장
        userFromToken.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(userFromToken);
    }
}   