package com.example.backend.admin.service;

import com.example.backend.admin.dto.UserAdminDto;
import com.example.backend.admin.repository.UserRepository;
import com.example.backend.authlogin.domain.User;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;

    public Page<UserAdminDto> list(String name, String email, User.Role role, Pageable pageable) {
        return userRepository.findUsersWithFilters(name, email, role, pageable)
                .map(UserAdminDto::from);
    }

    public UserAdminDto get(Long id) {
        return UserAdminDto.from(userRepository.findById(id).orElseThrow());
    }

    public void updateRole(Long id, User.Role role) {
        User u = userRepository.findById(id).orElseThrow();
        u.setRole(role);
        userRepository.save(u);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    public void updateStatus(Long id, boolean active) {
        var user = userRepository.findById(id).orElseThrow();
        user.setActive(active);
        userRepository.save(user);
    }
}
