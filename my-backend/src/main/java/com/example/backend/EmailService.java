package com.example.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 메모리에 인증코드 임시 저장 (실제 운영환경에서는 Redis 등 사용 권장)
    private final ConcurrentHashMap<String, String> verificationCodes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> codeExpirationTimes = new ConcurrentHashMap<>();

    private static final int CODE_EXPIRATION_TIME = 5 * 60 * 1000; // 5분

    /**
     * 6자리 랜덤 인증코드 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 비밀번호 재설정용 인증코드 이메일 발송
     */
    public boolean sendPasswordResetCode(String toEmail) {
        try {
            log.info("이메일 발송 시작: {}", toEmail);
            
            String verificationCode = generateVerificationCode();
            log.info("인증코드 생성 완료: {} ({})", toEmail, verificationCode);
            
            // 인증코드와 만료시간 저장
            verificationCodes.put(toEmail, verificationCode);
            codeExpirationTimes.put(toEmail, System.currentTimeMillis() + CODE_EXPIRATION_TIME);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            log.info("메일 발신자 설정: {}", fromEmail);
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[호텔 예약 시스템] 비밀번호 재설정 인증코드");

            String htmlContent = buildPasswordResetEmailContent(verificationCode);
            helper.setText(htmlContent, true);

            log.info("메일 전송 시도: {} -> {}", fromEmail, toEmail);
            mailSender.send(message);
            log.info("비밀번호 재설정 인증코드 발송 완료: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("이메일 발송 실패: {} - 오류 타입: {} - 메시지: {}", toEmail, e.getClass().getSimpleName(), e.getMessage());
            log.error("상세 스택 트레이스:", e);
            return false;
        }
    }

    /**
     * 인증코드 검증
     */
    public boolean verifyCode(String email, String inputCode) {
        String storedCode = verificationCodes.get(email);
        Long expirationTime = codeExpirationTimes.get(email);

        if (storedCode == null || expirationTime == null) {
            log.warn("인증코드가 존재하지 않음: {}", email);
            return false;
        }

        if (System.currentTimeMillis() > expirationTime) {
            // 만료된 코드 제거
            verificationCodes.remove(email);
            codeExpirationTimes.remove(email);
            log.warn("인증코드 만료: {}", email);
            return false;
        }

        boolean isValid = storedCode.equals(inputCode);
        if (isValid) {
            // 검증 성공 시 코드 제거
            verificationCodes.remove(email);
            codeExpirationTimes.remove(email);
            log.info("인증코드 검증 성공: {}", email);
        } else {
            log.warn("인증코드 불일치: {}", email);
        }

        return isValid;
    }

    /**
     * 이메일 내용 HTML 템플릿
     */
    private String buildPasswordResetEmailContent(String verificationCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .code-box { background-color: #fff; border: 2px solid #007bff; padding: 20px; margin: 20px 0; text-align: center; }
                    .code { font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px; }
                    .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🏨 호텔 예약 시스템</h1>
                        <p>비밀번호 재설정 인증코드</p>
                    </div>
                    <div class="content">
                        <h2>안녕하세요!</h2>
                        <p>비밀번호 재설정을 위한 인증코드를 보내드립니다.</p>
                        <p>아래 인증코드를 입력하여 비밀번호 재설정을 완료해주세요.</p>
                        
                        <div class="code-box">
                            <p>인증코드</p>
                            <div class="code">%s</div>
                        </div>
                        
                        <p><strong>⚠️ 중요사항:</strong></p>
                        <ul>
                            <li>이 인증코드는 <strong>5분간</strong> 유효합니다.</li>
                            <li>본인이 요청하지 않았다면 이 이메일을 무시해주세요.</li>
                            <li>인증코드는 타인에게 공유하지 마세요.</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>이 이메일은 자동으로 발송된 메일입니다.</p>
                        <p>© 2025 호텔 예약 시스템. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(verificationCode);
    }

    /**
     * 만료된 인증코드 정리 (스케줄러로 주기적 실행 권장)
     */
    public void cleanupExpiredCodes() {
        long currentTime = System.currentTimeMillis();
        codeExpirationTimes.entrySet().removeIf(entry -> {
            if (entry.getValue() < currentTime) {
                verificationCodes.remove(entry.getKey());
                return true;
            }
            return false;
        });
        log.info("만료된 인증코드 정리 완료");
    }
}