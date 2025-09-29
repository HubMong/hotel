package com.example.backend.mypage.service;

import java.util.NoSuchElementException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional; // 💡 [필수] 트랜잭션 import

import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.authlogin.domain.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final LoginRepository loginRepository;
    private final PasswordEncoder passwordEncoder; 
    
    // ▼▼▼ [기존] 이메일 개별 수정 메서드 유지 (하위 호환성) ▼▼▼
    @Transactional
    public void updateEmail(Long id, String username, String newEmail) {
        // 1. 현재 사용자(토큰 기준) 정보를 DB에서 조회
        User userFromToken = loginRepository.findByEmail(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // 2. '본인 확인' 보안 로직
        if (!userFromToken.getId().equals(id)) {
            throw new AccessDeniedException("자신의 정보만 수정할 수 있습니다.");
        }

        // 3. 새로운 이메일이 이미 사용 중인지 확인
        if (loginRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 4. 이메일 변경 후 저장
        userFromToken.setEmail(newEmail);
        loginRepository.save(userFromToken);
    }
    
    // ▼▼▼ [기존] 비밀번호 개별 수정 메서드 유지 (하위 호환성 및 ID 기반 조회) ▼▼▼
    @Transactional
    public void changePassword(Long id, String username, String currentPassword, String newPassword) {
        // 1. 사용자 조회: ID 기준으로 조회하여 이메일 변경의 영향을 받지 않도록 함
        User userFromToken = loginRepository.findById(id) 
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. (ID 기준)"));
        
        // 2. 현재 비밀번호가 일치하는지 확인
        if (!passwordEncoder.matches(currentPassword, userFromToken.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다."); 
        }
        
        // 3. 새 비밀번호를 암호화하여 저장
        userFromToken.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(userFromToken);
    }
    
    // 🏆 [핵심 추가] 이메일/비밀번호 동시 수정을 위한 단일 트랜잭션 메서드
    @Transactional
    public void updateProfileAndPass(
        Long id, 
        String username, 
        String newEmail, 
        String currentPassword, 
        String newPassword) {

        // 1. 사용자 조회 (ID 기준)
        User user = loginRepository.findById(id) 
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        boolean isEmailChanged = newEmail != null && !newEmail.isEmpty() && !user.getEmail().equals(newEmail);
        boolean isPasswordChanged = newPassword != null && !newPassword.isEmpty();

        if (!isEmailChanged && !isPasswordChanged) {
            return; // 변경 사항이 없으면 종료
        }

        // 2. 이메일 변경 처리 (변경 요청이 있을 때만 로직 실행)
        if (isEmailChanged) {
            // 이메일 중복 확인
            if (loginRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
            user.setEmail(newEmail);
        }

        // 3. 비밀번호 변경 처리 (변경 요청이 있을 때만 로직 실행)
        if (isPasswordChanged) {
            if (currentPassword == null || currentPassword.isEmpty()) {
                throw new IllegalArgumentException("비밀번호를 변경하려면 현재 비밀번호를 입력해야 합니다.");
            }
            
            // 현재 비밀번호 검증 (실패 시 트랜잭션 롤백)
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다."); 
            }
            
            // 새 비밀번호 적용
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        // 4. 변경 사항 저장 (두 작업 중 하나라도 실패하면 전체 롤백됨)
        loginRepository.save(user);
    }
}