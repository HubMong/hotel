package com.example.backend.authlogin.service;

import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final LoginRepository loginRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 여기서 username은 시스템상에서 이메일을 의미합니다.
        return loginRepository.findByEmail(username)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(username + " -> 데이터베이스에서 찾을 수 없습니다."));
    }

    // DB의 User 엔티티 정보를 기반으로 Spring Security가 사용하는 UserDetails 객체를 생성합니다.
    private UserDetails createUserDetails(User user) {
        String role = "ROLE_" + user.getRole().name();
        
        // org.springframework.security.core.userdetails.User를 사용합니다.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword() != null ? user.getPassword() : "", // 소셜 로그인은 비밀번호가 null일 수 있으므로 방어 코드 추가
                Collections.singleton(new SimpleGrantedAuthority(role))
        );
    }
}