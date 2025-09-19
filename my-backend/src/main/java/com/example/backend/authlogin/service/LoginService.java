package com.example.backend.authlogin.service;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;

     // 회원가입
    @Transactional
    public User register(User user) {
        log.info("LoginService.register 시작 - Email: {}", user.getEmail());
        
        // 중복 이메일 체크 (대소문자 무관)
        String normalizedEmail = user.getEmail().toLowerCase().trim();
        if (loginRepository.existsByEmail(normalizedEmail)) {
            log.warn("이미 가입된 이메일: {}", normalizedEmail);
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        
        // 이메일 정규화
        user.setEmail(normalizedEmail);
        
        // 비밀번호 암호화
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            log.info("비밀번호 암호화 진행");
            user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        }
        
        log.info("사용자 저장 시도 - Email: {}", user.getEmail());
        User savedUser = loginRepository.saveAndFlush(user);
        log.info("사용자 저장 완료 - ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
        
        // 저장이 실제로 되었는지 재확인
        if (savedUser.getId() == null) {
            log.error("사용자 저장 실패 - ID가 생성되지 않음");
            throw new RuntimeException("사용자 저장에 실패했습니다.");
        }
        
        return savedUser;
    }

    // 로그인 (DB에서 사용자 검증)
    @Transactional(readOnly = true)
    public Optional<User> login(String email, String rawPassword) {
        if (email == null || email.trim().isEmpty() || rawPassword == null || rawPassword.trim().isEmpty()) {
            log.warn("로그인 실패 - 빈 이메일 또는 비밀번호");
            return Optional.empty();
        }
        
        String normalizedEmail = email.toLowerCase().trim();
        log.info("로그인 시도 - Email: {}", normalizedEmail);
        
        Optional<User> userOpt = loginRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            log.warn("로그인 실패 - 사용자를 찾을 수 없음: {}", normalizedEmail);
            return Optional.empty();
        }
        
        User user = userOpt.get();
        log.info("사용자 발견 - ID: {}, Provider: {}", user.getId(), user.getProvider());
        
        // 소셜 로그인 사용자는 일반 로그인 불가
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            log.warn("로그인 실패 - 소셜 로그인 사용자: {}", normalizedEmail);
            return Optional.empty();
        }
        
        // 비밀번호 검증
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());
        log.info("비밀번호 검증 결과: {}", passwordMatches);
        
        if (passwordMatches) {
            log.info("로그인 성공 - Email: {}", normalizedEmail);
            return Optional.of(user);
        } else {
            log.warn("로그인 실패 - 비밀번호 불일치: {}", normalizedEmail);
            return Optional.empty();
        }
    }

    // 사용자 조회
    public Optional<User> findById(Long id) {
        return loginRepository.findById(id);
    }

    // 비밀번호 변경
    public boolean changePassword(Long userId, String newPassword) {
        try {
            Optional<User> userOpt = loginRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            
            // 소셜 로그인 사용자는 비밀번호 변경 불가
            if (user.getProvider() != User.Provider.LOCAL) {
                return false;
            }

            // 새 비밀번호 암호화 후 저장
            user.setPassword(passwordEncoder.encode(newPassword));
            loginRepository.save(user);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}