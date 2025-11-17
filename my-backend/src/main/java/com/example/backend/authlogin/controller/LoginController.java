package com.example.backend.authlogin.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.backend.authlogin.config.JwtUtil;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.authlogin.service.EmailService;
import com.example.backend.authlogin.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "https://hwiyeong.shop"}, allowCredentials = "true")
public class LoginController {
    
    private final LoginService loginService;
    private final JwtUtil jwtUtil;
    private final LoginRepository loginRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate;
    @Value("${RECAPTCHA_SECRET:}")
    private String recaptchaSecret;

    @Value("${RECAPTCHA_VERIFY_URL:https://www.google.com/recaptcha/api/siteverify}")
    private String recaptchaVerifyUrl;

    @Value("${RECAPTCHA_SITE_KEY:${VITE_RECAPTCHA_SITE_KEY:}}")
    private String recaptchaSiteKey;

    private static final Duration SHORT_WINDOW = Duration.ofMinutes(15);
    private static final Duration LONG_WINDOW = Duration.ofHours(24);
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int SHORT_LOCK_THRESHOLD = 5;
    private static final int LONG_LOCK_THRESHOLD = 10;
    private static final Duration SHORT_LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration LONG_LOCK_DURATION = Duration.ofHours(24);
    private static final String UNKNOWN_USER_KEY = "__unknown_user__";
    private static final String UNKNOWN_IP_KEY = "__unknown_ip__";

    private static final Map<String, Deque<Instant>> USER_FAILURES = new ConcurrentHashMap<>();
    private static final Map<String, Deque<Instant>> IP_FAILURES = new ConcurrentHashMap<>();
    private static final Map<String, Instant> SHORT_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, Instant> LONG_LOCKS = new ConcurrentHashMap<>();
    
    // 회원가입
    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request == null) {
            log.warn("Registration failed - Request body is null");
            return ResponseEntity.badRequest().body("회원가입 데이터가 필요합니다.");
        }

        String recaptchaToken = request.getRecaptchaToken();
        if (recaptchaToken == null || recaptchaToken.trim().isEmpty()) {
            log.warn("Registration failed - Missing reCAPTCHA token (email: {})", request.getEmail());
            return ResponseEntity.badRequest().body("reCAPTCHA 토큰이 필요합니다.");
        }

        if (!verifyRecaptchaToken(recaptchaToken.trim())) {
            log.warn("Registration failed - reCAPTCHA verification failed (email: {})", request.getEmail());
            return ResponseEntity.badRequest().body("reCAPTCHA 검증에 실패했습니다.");
        }

        User user;
        try {
            user = request.toUser();
        } catch (IllegalArgumentException e) {
            log.warn("Registration failed - {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        log.info("User registration attempt for email: {}", user.getEmail());
        log.info("User registration data - Name: {}, Email: {}, Phone: {}, Address: {}", 
                user.getName(), user.getEmail(), user.getPhone(), user.getAddress());
        
        // 필수 필드 검증
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            log.warn("Registration failed - Name is required");
            return ResponseEntity.badRequest().body("이름을 입력해주세요.");
        }
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            log.warn("Registration failed - Email is required");
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            log.warn("Registration failed - Password is required");
            return ResponseEntity.badRequest().body("비밀번호를 입력해주세요.");
        }
        
        // 이메일 형식 검증
        if (!isValidEmail(user.getEmail())) {
            log.warn("Registration failed - Invalid email format: {}", user.getEmail());
            return ResponseEntity.badRequest().body("올바른 이메일 형식을 입력해주세요.");
        }
        
        // 비밀번호 최소 길이 검증
        if (user.getPassword().length() < 6) {
            log.warn("Registration failed - Password too short");
            return ResponseEntity.badRequest().body("비밀번호는 최소 6자 이상이어야 합니다.");
        }
        
        try {
            User savedUser = loginService.register(user);
            log.info("User registration successful - ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException e) {
            log.error("User registration failed for email: {} - {}", user.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("User registration failed for email: {} - Error: {}", user.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(500).body("회원가입 중 오류가 발생했습니다.");
        }
    }

    // 로그인
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse httpResponse) {
        String email = request != null ? request.getEmail() : null;
        String password = request != null ? request.getPassword() : null;
        String recaptchaToken = request != null ? request.getRecaptchaToken() : null;
        log.info("Login attempt for email: {}", email);

        Instant now = Instant.now();
        String clientIp = resolveClientIp(httpRequest);
    String normalizedEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
        String emailKey = normalizedEmail != null && !normalizedEmail.isEmpty() ? normalizedEmail : UNKNOWN_USER_KEY;
        String ipKey = clientIp != null && !clientIp.isEmpty() ? clientIp : UNKNOWN_IP_KEY;
        Supplier<Optional<User>> userSupplier = memoizedUserSupplier(normalizedEmail);

        if (isLocked(LONG_LOCKS, emailKey, now, userSupplier)) {
            log.warn("Login blocked (24h lock) for email: {} from IP: {}", normalizedEmail, clientIp);
            return ResponseEntity.status(423).body("보안상의 이유로 24시간 동안 로그인 시도가 제한되었습니다. 고객센터로 문의해 주세요.");
        }

        if (isLocked(SHORT_LOCKS, emailKey, now, userSupplier)) {
            log.warn("Login blocked (15min lock) for email: {} from IP: {}", normalizedEmail, clientIp);
            return ResponseEntity.status(423).body("로그인 시도가 반복적으로 실패하여 15분 동안 로그인이 제한되었습니다. 안내 이메일을 확인해주세요.");
        }

        boolean captchaRequired = isCaptchaRequired(emailKey, ipKey, now);
        if (captchaRequired) {
            if (recaptchaToken == null || recaptchaToken.trim().isEmpty()) {
                log.warn("Login failed for email: {} - Missing reCAPTCHA token (required)", normalizedEmail);
                return ResponseEntity.badRequest().body("보안을 위해 reCAPTCHA 인증이 필요합니다.");
            }

            if (!verifyRecaptchaToken(recaptchaToken.trim())) {
                log.warn("Login failed for email: {} - reCAPTCHA verification failed", normalizedEmail);
                return ResponseEntity.badRequest().body("reCAPTCHA 검증에 실패했습니다.");
            }
        }

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        }

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호를 입력해주세요.");
        }

        try {
            Optional<User> user = loginService.loginWithPreloadedUser(userSupplier.get().orElse(null), password);

            if (user.isPresent()) {
                String token = jwtUtil.generateToken(user.get());
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", getUserInfo(user.get()));

        ResponseCookie authCookie = ResponseCookie
            .from("token", token)
            .maxAge(Duration.ofHours(1))
            .sameSite("None")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());

        log.info("Login successful for email: {}", email);
        resetFailures(emailKey, ipKey, userSupplier);
        return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for email: {} - Invalid credentials", email);
                registerFailure(emailKey, ipKey, now, userSupplier);
                return handleFailureResponse(normalizedEmail, emailKey, ipKey, now, userSupplier);
            }
        } catch (Exception e) {
            log.error("Login error for email: {} - {}", email, e.getMessage());
            return ResponseEntity.status(500).body("로그인 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/config/recaptcha-site-key")
    public ResponseEntity<?> getRecaptchaSiteKey() {
        if (recaptchaSiteKey == null || recaptchaSiteKey.trim().isEmpty()) {
            log.error("reCAPTCHA site key is not configured. Set RECAPTCHA_SITE_KEY in the environment.");
            return ResponseEntity.status(500).body(Map.of(
                    "message", "reCAPTCHA 사이트 키가 설정되지 않았습니다."
            ));
        }

        return ResponseEntity.ok(Map.of("siteKey", recaptchaSiteKey));
    }

    public static class LoginRequest {
        private String email;
        private String password;
        private String recaptchaToken;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRecaptchaToken() {
            return recaptchaToken;
        }

        public void setRecaptchaToken(String recaptchaToken) {
            this.recaptchaToken = recaptchaToken;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isLocked(Map<String, Instant> lockMap, String key, Instant now,
                             Supplier<Optional<User>> userSupplier) {
        Instant expiry = lockMap.get(key);
        if (expiry == null) {
            Optional<User> userOpt = resolveUser(key, userSupplier);
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            LocalDateTime lockUntil = user.getLoginLockUntil();
            User.LoginLockType lockType = user.getLoginLockType();
            if (lockUntil != null && lockType != null) {
                Instant dbExpiry = toInstant(lockUntil);
                if (dbExpiry.isAfter(now)) {
                    if (lockType == User.LoginLockType.LONG && lockMap == LONG_LOCKS) {
                        LONG_LOCKS.put(key, dbExpiry);
                    } else if (lockType == User.LoginLockType.SHORT && lockMap == SHORT_LOCKS) {
                        SHORT_LOCKS.put(key, dbExpiry);
                    }
                    expiry = dbExpiry;
                } else {
                    clearUserLockIfExpired(key, userSupplier);
                    return false;
                }
            } else {
                return false;
            }
        }

        if (expiry.isAfter(now)) {
            synchronizeUserLockState(key, expiry,
                    lockMap == LONG_LOCKS ? User.LoginLockType.LONG : User.LoginLockType.SHORT,
                    userSupplier);
            return true;
        }
        lockMap.remove(key, expiry);
        clearUserLockIfExpired(key, userSupplier);
        return false;
    }

    private Supplier<Optional<User>> memoizedUserSupplier(String normalizedEmail) {
        return new Supplier<>() {
            private boolean initialized;
            private Optional<User> cached = Optional.empty();

            @Override
            public Optional<User> get() {
                if (!initialized) {
                    if (normalizedEmail == null || normalizedEmail.isBlank()) {
                        cached = Optional.empty();
                    } else {
                        cached = loginRepository.findByEmail(normalizedEmail);
                    }
                    initialized = true;
                }
                return cached;
            }
        };
    }

    private Optional<User> resolveUser(String emailKey, Supplier<Optional<User>> userSupplier) {
        if (emailKey == null || UNKNOWN_USER_KEY.equals(emailKey) || userSupplier == null) {
            return Optional.empty();
        }
        Optional<User> cached = userSupplier.get();
        if (cached.isPresent() && cached.get().getEmail() != null
                && cached.get().getEmail().equalsIgnoreCase(emailKey)) {
            return cached;
        }
        return loginRepository.findByEmail(emailKey);
    }

    private boolean isCaptchaRequired(String emailKey, String ipKey, Instant now) {
        int userFailures = countRecentFailures(USER_FAILURES, emailKey, SHORT_WINDOW, now);
        int ipFailures = countRecentFailures(IP_FAILURES, ipKey, SHORT_WINDOW, now);
        return userFailures >= CAPTCHA_THRESHOLD || ipFailures >= CAPTCHA_THRESHOLD;
    }

    private void registerFailure(String emailKey, String ipKey, Instant now,
                                 Supplier<Optional<User>> userSupplier) {
        appendFailure(USER_FAILURES, emailKey, now);
        appendFailure(IP_FAILURES, ipKey, now);

        Optional<User> userOpt = resolveUser(emailKey, userSupplier);

        int recentUserFailures = countRecentFailures(USER_FAILURES, emailKey, SHORT_WINDOW, now);
        int longUserFailures = countRecentFailures(USER_FAILURES, emailKey, LONG_WINDOW, now);

        if (longUserFailures >= LONG_LOCK_THRESHOLD) {
            Instant longLockExpiry = now.plus(LONG_LOCK_DURATION);
            LONG_LOCKS.put(emailKey, longLockExpiry);
            SHORT_LOCKS.remove(emailKey);
            userOpt.ifPresent(user -> {
                user.setLoginLockType(User.LoginLockType.LONG);
                user.setLoginLockUntil(toLocalDateTime(longLockExpiry));
                loginRepository.save(user);
            });
            log.warn("24-hour login lock applied for email key: {}", emailKey);
            return;
        }

        if (recentUserFailures >= SHORT_LOCK_THRESHOLD) {
            Instant shortLockExpiry = now.plus(SHORT_LOCK_DURATION);
            Instant previousExpiry = SHORT_LOCKS.put(emailKey, shortLockExpiry);
            if (!UNKNOWN_USER_KEY.equals(emailKey)) {
                userOpt.ifPresent(user -> {
                    user.setLoginLockType(User.LoginLockType.SHORT);
                    user.setLoginLockUntil(toLocalDateTime(shortLockExpiry));
                    loginRepository.save(user);
                });
                if (previousExpiry == null || previousExpiry.isBefore(now)) {
                    userOpt.ifPresent(user -> emailService.sendLoginLockNotification(user.getEmail(), SHORT_LOCK_DURATION.toMinutes()));
                }
            }
            log.warn("15-minute login lock applied for email key: {}", emailKey);
        }
    }

    private ResponseEntity<?> handleFailureResponse(String normalizedEmail, String emailKey, String ipKey,
                                                   Instant now, Supplier<Optional<User>> userSupplier) {
        if (isLocked(LONG_LOCKS, emailKey, now, userSupplier)) {
            return ResponseEntity.status(423).body("보안상의 이유로 24시간 동안 로그인 시도가 제한되었습니다. 고객센터로 문의해 주세요.");
        }

        if (isLocked(SHORT_LOCKS, emailKey, now, userSupplier)) {
            return ResponseEntity.status(423).body("로그인 시도가 반복적으로 실패하여 15분 동안 로그인이 제한되었습니다. 안내 이메일을 확인해주세요.");
        }

        int userFailures = countRecentFailures(USER_FAILURES, emailKey, SHORT_WINDOW, now);
        int ipFailures = countRecentFailures(IP_FAILURES, ipKey, SHORT_WINDOW, now);
        if (userFailures >= CAPTCHA_THRESHOLD || ipFailures >= CAPTCHA_THRESHOLD) {
            log.info("reCAPTCHA required for future login attempts - email: {}", normalizedEmail);
            return ResponseEntity.status(401).body("보안을 위해 reCAPTCHA 인증이 필요합니다.");
        }

        return ResponseEntity.status(401).body("로그인 실패: 이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    private void resetFailures(String emailKey, String ipKey, Supplier<Optional<User>> userSupplier) {
        if (emailKey != null) {
            USER_FAILURES.remove(emailKey);
            SHORT_LOCKS.remove(emailKey);
            LONG_LOCKS.remove(emailKey);
            if (!UNKNOWN_USER_KEY.equals(emailKey)) {
                resolveUser(emailKey, userSupplier).ifPresent(user -> {
                    if (user.getLoginLockUntil() != null || user.getLoginLockType() != null) {
                        user.setLoginLockUntil(null);
                        user.setLoginLockType(null);
                        loginRepository.save(user);
                    }
                });
            }
        }
        if (ipKey != null) {
            IP_FAILURES.remove(ipKey);
        }
    }

    private void appendFailure(Map<String, Deque<Instant>> store, String key, Instant timestamp) {
        if (key == null) {
            return;
        }
        Deque<Instant> deque = store.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(timestamp);
        pruneOldEntries(deque, LONG_WINDOW, timestamp);
    }

    private int countRecentFailures(Map<String, Deque<Instant>> store, String key, Duration window, Instant now) {
        if (key == null) {
            return 0;
        }
        Deque<Instant> deque = store.get(key);
        if (deque == null) {
            return 0;
        }
        pruneOldEntries(deque, LONG_WINDOW, now);
        Instant cutoff = now.minus(window);
        int count = 0;
        for (Instant instant : deque) {
            if (!instant.isBefore(cutoff)) {
                count++;
            }
        }
        return count;
    }

    private void pruneOldEntries(Deque<Instant> deque, Duration window, Instant now) {
        if (deque == null) {
            return;
        }
        Instant cutoff = now.minus(window);
        while (true) {
            Instant oldest = deque.peekFirst();
            if (oldest == null || !oldest.isBefore(cutoff)) {
                break;
            }
            deque.pollFirst();
        }
    }

    private void synchronizeUserLockState(String emailKey, Instant expiry, User.LoginLockType lockType,
                                          Supplier<Optional<User>> userSupplier) {
        if (emailKey == null || UNKNOWN_USER_KEY.equals(emailKey)) {
            return;
        }
        LocalDateTime until = toLocalDateTime(expiry);
        resolveUser(emailKey, userSupplier).ifPresent(user -> {
            LocalDateTime currentUntil = user.getLoginLockUntil();
            User.LoginLockType currentType = user.getLoginLockType();
            if (currentUntil == null || currentUntil.isBefore(until) || currentType != lockType) {
                user.setLoginLockType(lockType);
                user.setLoginLockUntil(until);
                loginRepository.save(user);
            }
        });
    }

    private void clearUserLockIfExpired(String emailKey, Supplier<Optional<User>> userSupplier) {
        if (emailKey == null || UNKNOWN_USER_KEY.equals(emailKey)) {
            return;
        }
        resolveUser(emailKey, userSupplier).ifPresent(user -> {
            if (user.getLoginLockUntil() != null || user.getLoginLockType() != null) {
                user.setLoginLockUntil(null);
                user.setLoginLockType(null);
                loginRepository.save(user);
            }
        });
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    public static void clearLockStateForEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
    String key = email.trim().toLowerCase(Locale.ROOT);
        USER_FAILURES.remove(key);
        SHORT_LOCKS.remove(key);
        LONG_LOCKS.remove(key);
    }

    public static class RegisterRequest {
        private String name;
        private String email;
        private String password;
        private String phone;
        private String address;
        private String dateOfBirth;
        private String recaptchaToken;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public String getRecaptchaToken() {
            return recaptchaToken;
        }

        public void setRecaptchaToken(String recaptchaToken) {
            this.recaptchaToken = recaptchaToken;
        }

        public User toUser() {
            User user = new User();
            user.setName(this.name);
            user.setEmail(this.email);
            user.setPassword(this.password);
            user.setPhone(this.phone);
            user.setAddress(this.address);
            if (this.dateOfBirth != null && !this.dateOfBirth.trim().isEmpty()) {
                try {
                    LocalDate dob = LocalDate.parse(this.dateOfBirth.trim());
                    user.setDateOfBirth(dob);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("올바른 생일 형식을 입력해주세요. (예: 1990-01-01)");
                }
            }
            user.setProvider(User.Provider.LOCAL);
            user.setRole(User.Role.USER);
            return user;
        }
    }

    // 사용자 정보 조회 (JWT 토큰 기반)
    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "token", required = false) String cookieToken) {
        try {
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else if (cookieToken != null && !cookieToken.isBlank()) {
                token = cookieToken;
            }

            if (token == null) {
                return ResponseEntity.status(401).body("Token is required");
            }

            String email = jwtUtil.extractEmail(token);
            
            if (!jwtUtil.validateToken(token, email)) {
                return ResponseEntity.status(401).body("Token validation failed");
            }

            User user = loginRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(getUserInfo(user));
            
        } catch (Exception e) {
            log.error("Error getting user info: {}", e.getMessage());
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    @PostMapping("/users/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie invalidate = ResponseCookie
                .from("token", "")
                .maxAge(0)
                .sameSite("None")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, invalidate.toString());
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }

    // 사용자 ID로 조회
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return loginService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> getUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail());
    userInfo.put("phone", user.getPhone());
        userInfo.put("provider", user.getProvider().toString());
        userInfo.put("profileImageUrl", user.getProfileImageUrl());
        userInfo.put("createdOn", user.getCreatedOn());
        if (user.getRole() != null) {
            userInfo.put("role", user.getRole().name());
        }
        return userInfo;
    }

    // 비밀번호 재설정 - 이메일 인증코드 발송
    @PostMapping("/password/reset/send-code")
    public ResponseEntity<?> sendPasswordResetCode(@RequestBody Map<String, String> request) {
        String email = request != null ? request.get("email") : null;
        log.info("Password reset code request for email: {}", email);
        
        try {
            // 이메일 형식 검증
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "올바른 이메일 주소를 입력해주세요."
                ));
            }

            // 사용자 존재 여부 확인
            log.info("사용자 검색 시작: {}", email.trim());
            Optional<User> userOpt = loginRepository.findByEmail(email.trim());
            log.info("사용자 검색 결과: {}", userOpt.isPresent());
            
            if (userOpt.isEmpty()) {
                log.warn("존재하지 않는 사용자 이메일: {}", email.trim());
                // 보안상 실제로는 존재하지 않는 이메일이어도 성공 메시지 반환
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증코드가 발송되었습니다. 이메일을 확인해주세요."
                ));
            }

            // 소셜 로그인 사용자는 비밀번호 재설정 불가
            User user = userOpt.get();
            log.info("사용자 정보 확인 - Provider: {}, Email: {}", user.getProvider(), user.getEmail());
            if (user.getProvider() != User.Provider.LOCAL) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "소셜 로그인 계정은 비밀번호 재설정이 불가능합니다."
                ));
            }

            // 인증코드 발송
            log.info("EmailService.sendVerificationCode 호출 직전: {}", email.trim());
            String result = emailService.sendVerificationCode(email.trim());
            log.info("EmailService.sendVerificationCode 호출 결과: {}", result);
            
            if (result.contains("발송되었습니다")) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증코드가 발송되었습니다. 이메일을 확인해주세요."
                ));
            } else {
                return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."
                ));
            }

        } catch (Exception e) {
            log.error("Password reset code sending failed for email: {} - {}", email, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            ));
        }
    }

    // 비밀번호 재설정 - 인증코드 검증 및 비밀번호 변경
    @PostMapping("/password/reset/verify-and-change")
    public ResponseEntity<?> verifyCodeAndChangePassword(@RequestBody Map<String, String> request) {
        String email = request != null ? request.get("email") : null;
        String verificationCode = request != null ? request.get("verificationCode") : null;
        String newPassword = request != null ? request.get("newPassword") : null;
        
        log.info("Password reset verification for email: {}", email);
        
        try {
            // 입력값 검증
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
                ));
            }

            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "인증코드를 입력해주세요."
                ));
            }

            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "새 비밀번호는 6자 이상이어야 합니다."
                ));
            }

            // 인증코드 검증 및 비밀번호 재설정
            String resetResult = emailService.verifyCodeAndResetPassword(email.trim(), verificationCode.trim(), newPassword);
            if (resetResult.contains("성공적으로")) {
                log.info("Password successfully changed for user: {}", email);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "비밀번호가 성공적으로 변경되었습니다."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", resetResult
                ));
            }

        } catch (Exception e) {
            log.error("Password reset failed for email: {} - {}", email, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            ));
        }
    }
    
    // 이메일 형식 검증 메소드
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean verifyRecaptchaToken(String token) {
        if (recaptchaSecret == null || recaptchaSecret.trim().isEmpty()) {
            log.error("reCAPTCHA secret is not configured. Set RECAPTCHA_SECRET in the environment.");
            return false;
        }

        if (recaptchaVerifyUrl == null || recaptchaVerifyUrl.trim().isEmpty()) {
            log.error("reCAPTCHA verify URL is not configured. Set RECAPTCHA_VERIFY_URL in the environment.");
            return false;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", recaptchaSecret);
        body.add("response", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(recaptchaVerifyUrl, request, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            log.error("reCAPTCHA verification error: {}", e.getMessage());
            return false;
        }
    }
}
