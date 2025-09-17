package com.example.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:5173")
public class TestController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private String mailPort;

    @Value("${spring.mail.username}")
    private String mailUsername;

    /**
     * 메일 설정 확인용 엔드포인트
     */
    @GetMapping("/mail-config")
    public Map<String, Object> getMailConfig() {
        return Map.of(
            "mailHost", mailHost,
            "mailPort", mailPort,
            "mailUsername", mailUsername,
            "mailSender", mailSender != null ? "OK" : "NULL"
        );
    }

    /**
     * 이메일 발송 테스트용 엔드포인트
     */
    @PostMapping("/send-email")
    public Map<String, Object> testSendEmail(@RequestBody Map<String, String> request) {
        String email = request.get("to");
        log.info("=== 이메일 발송 테스트 시작 ===");
        log.info("대상 이메일: {}", email);
        log.info("메일 서버 설정: {}:{}", mailHost, mailPort);
        log.info("메일 사용자: {}", mailUsername);
        
        try {
            boolean result = emailService.sendPasswordResetCode(email);
            
            if (result) {
                return Map.of(
                    "success", true,
                    "message", "테스트 이메일이 성공적으로 발송되었습니다.",
                    "config", Map.of(
                        "host", mailHost,
                        "port", mailPort,
                        "username", mailUsername
                    )
                );
            } else {
                return Map.of(
                    "success", false,
                    "message", "이메일 발송에 실패했습니다. 로그를 확인해주세요."
                );
            }
        } catch (Exception e) {
            log.error("이메일 발송 테스트 중 오류: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "서버 오류: " + e.getMessage()
            );
        }
    }
}