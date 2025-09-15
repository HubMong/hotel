package com.example.backend;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;

     // 회원가입
    public User register(User user) {
        if (loginRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        
        // 비밀번호 암호화
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        return loginRepository.save(user);
    }

    // 로그인 (DB에서 사용자 검증)
    public Optional<User> login(String email, String rawPassword) {
        if (email == null || email.trim().isEmpty() || rawPassword == null || rawPassword.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return loginRepository.findByEmail(email.trim())
                .filter(user -> {
                    // 소셜 로그인 사용자는 일반 로그인 불가
                    if (user.getPassword() == null) {
                        return false;
                    }
                    // 비밀번호 검증
                    return passwordEncoder.matches(rawPassword, user.getPassword());
                });
    }

    // 사용자 조회
    public Optional<User> findById(Long id) {
        return loginRepository.findById(id);
    }
}