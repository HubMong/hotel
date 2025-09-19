package com.example.backend.authlogin.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final LoginRepository loginRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    // 임시 저장소: 실제 환경에서는 Redis 등 사용 권장
    private static final java.util.Map<String, VerificationCode> codeStorage = new java.util.concurrent.ConcurrentHashMap<>();

    public String sendVerificationCode(String email) {
        try {
            // 이메일 존재 확인
            if (!loginRepository.existsByEmail(email.toLowerCase().trim())) {
                log.warn("비밀번호 재설정 요청 - 존재하지 않는 이메일: {}", email);
                return "존재하지 않는 이메일입니다.";
            }
            
            // 인증 코드 생성 (6자리 숫자)
            String code = generateVerificationCode();
            
            // 코드 저장 (5분 유효)
            codeStorage.put(email.toLowerCase().trim(), new VerificationCode(code, LocalDateTime.now().plusMinutes(5)));
            
            // 이메일 발송
            sendEmail(email, "비밀번호 재설정 인증 코드", buildEmailContent(code));
            
            log.info("인증 코드 발송 완료: {}", email);
            return "인증 코드가 발송되었습니다.";
            
        } catch (Exception e) {
            log.error("인증 코드 발송 실패: {} - {}", email, e.getMessage());
            return "인증 코드 발송에 실패했습니다.";
        }
    }
    
    public String verifyCodeAndResetPassword(String email, String code, String newPassword) {
        try {
            String normalizedEmail = email.toLowerCase().trim();
            
            // 코드 확인
            VerificationCode storedCode = codeStorage.get(normalizedEmail);
            if (storedCode == null) {
                log.warn("인증 코드 불일치 - 코드 없음: {}", normalizedEmail);
                return "인증 코드가 유효하지 않습니다.";
            }
            
            // 만료 시간 확인
            if (LocalDateTime.now().isAfter(storedCode.expiryTime)) {
                codeStorage.remove(normalizedEmail);
                log.warn("인증 코드 만료: {}", normalizedEmail);
                return "인증 코드가 만료되었습니다.";
            }
            
            // 코드 일치 확인
            if (!storedCode.code.equals(code)) {
                log.warn("인증 코드 불일치: {}", normalizedEmail);
                return "인증 코드가 일치하지 않습니다.";
            }
            
            // 비밀번호 재설정
            var userOpt = loginRepository.findByEmail(normalizedEmail);
            if (userOpt.isEmpty()) {
                codeStorage.remove(normalizedEmail);
                return "사용자를 찾을 수 없습니다.";
            }
            
            User user = userOpt.get();
            
            // 소셜 로그인 사용자 체크
            if (user.getProvider() != User.Provider.LOCAL) {
                codeStorage.remove(normalizedEmail);
                return "소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.";
            }
            
            // 비밀번호 암호화 후 저장
            user.setPassword(passwordEncoder.encode(newPassword));
            loginRepository.save(user);
            
            // 사용된 코드 삭제
            codeStorage.remove(normalizedEmail);
            
            log.info("비밀번호 재설정 완료: {}", normalizedEmail);
            return "비밀번호가 성공적으로 변경되었습니다.";
            
        } catch (Exception e) {
            log.error("비밀번호 재설정 실패: {} - {}", email, e.getMessage());
            return "비밀번호 재설정에 실패했습니다.";
        }
    }
    
    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    private void sendEmail(String to, String subject, String content) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        helper.setFrom("noreply@yourapp.com");
        
        mailSender.send(message);
    }
    
    private String buildEmailContent(String code) {
        return String.format(
            "<div style='max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif;'>" +
            "<h2 style='color: #333; text-align: center;'>비밀번호 재설정 인증 코드</h2>" +
            "<div style='background-color: #f8f9fa; padding: 20px; border-radius: 8px; text-align: center;'>" +
            "<h3 style='color: #007bff; font-size: 24px; letter-spacing: 3px;'>%s</h3>" +
            "<p style='color: #666; margin-top: 15px;'>위 인증 코드를 입력하여 비밀번호를 재설정하세요.</p>" +
            "<p style='color: #999; font-size: 12px;'>이 코드는 5분 후에 만료됩니다.</p>" +
            "</div>" +
            "</div>", 
            code
        );
    }
    
    // 내부 클래스로 인증 코드 데이터 구조 정의
    private static class VerificationCode {
        String code;
        LocalDateTime expiryTime;
        
        VerificationCode(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }
}