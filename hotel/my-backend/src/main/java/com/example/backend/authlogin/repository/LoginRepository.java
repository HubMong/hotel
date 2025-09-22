package com.example.backend.authlogin.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.authlogin.domain.User;

public interface LoginRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    List<User> findAllByEmail(String email); // 중복 계정 조회용
    boolean existsByEmail(String email);
    
    // 소셜 로그인용 메서드
    Optional<User> findByEmailAndProvider(String email, User.Provider provider);
    Optional<User> findByProviderIdAndProvider(String providerId, User.Provider provider);
}