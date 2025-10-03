// src/main/java/com/example/backend/payment/controller/PaymentController.java
package com.example.backend.payment.controller;

import com.example.backend.authlogin.domain.User;
import com.example.backend.admin.repository.AdminUserRepository;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.payment.domain.Payment;
import com.example.backend.payment.dto.PaymentDTOs.TossPaymentResponse;
import com.example.backend.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentRepository repository;
    private final ReservationRepository reservationRepository;
    private final AdminUserRepository userRepository;
    private final RestTemplate restTemplate;

    public PaymentController(
            PaymentRepository repository,
            ReservationRepository reservationRepository,
            AdminUserRepository userRepository,
            RestTemplate restTemplate
    ) {
        this.repository = repository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? String.valueOf(auth.getPrincipal()) : null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    /** 특정 예약의 최신 결제(표시/취소용) */
    @GetMapping("/{reservationId}/byreservation")
    public ResponseEntity<?> getLatestByReservation(@PathVariable Long reservationId) {
        return repository.findTopByReservationIdOrderByCreatedAtDesc(reservationId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "payment not found")));
    }

    /** 결제 요청 생성 (PENDING) */
    @Transactional
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Payment payload) {
        String email = currentUserEmail();
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "unauthenticated"));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "user not found"));
        }

        if (payload.getOrderId() != null && repository.existsByOrderId(payload.getOrderId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "duplicate orderId"));
        }

        // 예약 존재 여부 확인 (FK 무결성)
        Reservation reservation = reservationRepository.findById(payload.getReservationId()).orElse(null);
        if (reservation == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "reservation not found"));
        }

        Payment p = Payment.builder()
                .reservationId(payload.getReservationId())
                .userId(user.getId())  // userId 명시적 설정 (FK 무결성)
                .orderId(payload.getOrderId())
                .orderName(payload.getOrderName())
                .paymentMethod(Optional.ofNullable(payload.getPaymentMethod()).orElse("TOSS_PAY"))
                .basePrice(payload.getBasePrice() != null ? payload.getBasePrice() : 0)
                .tax(payload.getTax() != null ? payload.getTax() : 0)
                .discount(payload.getDiscount() != null ? payload.getDiscount() : 0)
                .amount(payload.getAmount())
                .paymentKey(payload.getPaymentKey())
                .build();

        p.setUser(user);  // 연관관계 설정 (userId는 builder에서 이미 설정됨)
        p.setStatus(Payment.Status.PENDING);

        repository.save(p);
        return ResponseEntity.ok(p);
    }

    /** 결제 승인 + 예약 확정 */
    @Transactional
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody Map<String, Object> body) {
        String orderId    = String.valueOf(body.get("orderId"));
        Integer amount    = Integer.valueOf(String.valueOf(body.get("amount")));
        String paymentKey = String.valueOf(body.get("paymentKey"));

        Payment pay = repository.findByOrderId(orderId);
        if (pay == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "PAYMENT_NOT_FOUND"));
        }
        if (!Objects.equals(pay.getAmount(), amount)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "AMOUNT_MISMATCH"));
        }

        Reservation rv = reservationRepository.findById(pay.getReservationId()).orElse(null);
        if (rv == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "RESERVATION_NOT_FOUND"));
        }
        if (rv.getStatus() == Reservation.Status.CANCELLED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "RESERVATION_CANCELLED"));
        }
        if (rv.getStatus() == Reservation.Status.COMPLETED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "RESERVATION_COMPLETED"));
        }

        // 토스 결제 승인
        pay.setPaymentKey(paymentKey);
        String url = "https://api.tosspayments.com/v1/payments/confirm";
        String encryptedKey = "Basic " + Base64.getEncoder()
                .encodeToString("test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6:".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", encryptedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format(
                "{\"orderId\":\"%s\",\"amount\":%d,\"paymentKey\":\"%s\"}",
                pay.getOrderId(), amount, pay.getPaymentKey()
        );

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class
        );

        if (resp.getStatusCode() == HttpStatus.OK
                && resp.getBody() != null
                && resp.getBody().contains("\"status\":\"DONE\"")) {

            // 토스 응답 → 결제수단/영수증 저장
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(resp.getBody());

                String method       = root.path("method").asText(null);
                String easyProvider = root.path("easyPay").path("provider").asText(null);
                String receiptUrl   = root.path("receipt").path("url").asText(null);

                String computedMethod;
                if (easyProvider != null && !easyProvider.isBlank()) {
                    computedMethod = "TOSS:" + easyProvider;
                } else if (method != null && !method.isBlank()) {
                    computedMethod = "TOSS:" + method;
                } else {
                    computedMethod = "TOSS";
                }

                pay.setPaymentMethod(computedMethod);
                if (receiptUrl != null && !receiptUrl.isBlank()) {
                    pay.setReceiptUrl(receiptUrl);
                }
            } catch (Exception ignore) {}

            pay.setStatus(Payment.Status.COMPLETED);
            repository.save(pay);

            rv.setStatus(Reservation.Status.COMPLETED);
            rv.setTransactionId(paymentKey);
            reservationRepository.save(rv);

            return ResponseEntity.ok(Map.of(
                    "paymentId", pay.getId(),
                    "reservationId", rv.getId(),
                    "status", "COMPLETED"
            ));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("code", "TOSS_CONFIRM_FAILED"));
    }

    @GetMapping("/lists")
    public ResponseEntity<List<Payment>> getLists() {
        return ResponseEntity.ok(repository.findAll());
    }

    /** 토스 결제 상세 조회 */
    @Transactional
    @GetMapping("/view/{paymentId}")
    public ResponseEntity<?> viewPayment(@PathVariable Long paymentId) {
        Optional<Payment> optional = repository.findById(paymentId);
        if (optional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "not found"));
        }

        Payment p = optional.get();
        String url = "https://api.tosspayments.com/v1/payments/" + p.getPaymentKey();
        String encryptedKey = "Basic " + Base64.getEncoder()
                .encodeToString("test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6:".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", encryptedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class
        );
        try {
            ObjectMapper mapper = new ObjectMapper();
            TossPaymentResponse t = mapper.readValue(resp.getBody(), TossPaymentResponse.class);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", t.getOrderId());
            result.put("orderName", t.getOrderName());
            result.put("requestedAt", t.getRequestedAt());
            result.put("approvedAt", t.getApprovedAt());
            result.put("amount", t.getTotalAmount());
            result.put("status", t.getStatus());
            result.put("receiptUrl", t.getReceipt() != null ? t.getReceipt().getUrl() : null);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "parse error"));
        }
    }

    /** 결제 취소 + 예약 동기화 (멱등 처리) */
    @Transactional
    @PostMapping("/cancel/{paymentId}")
    public ResponseEntity<?> cancelPayment(
            @PathVariable Long paymentId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        Optional<Payment> optional = repository.findById(paymentId);
        if (optional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "not found"));
        }

        Payment p = optional.get();

        // ✅ 이미 취소면 200 OK (멱등)
        if (p.getStatus() == Payment.Status.CANCELLED) {
            Map<String, Object> out = new HashMap<>();
            out.put("message", "already cancelled");
            out.put("paymentId", p.getId());
            return ResponseEntity.ok(out);
        }

        if (p.getPaymentKey() == null || p.getPaymentKey().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "missing paymentKey"));
        }

        String cancelReason = (reason == null || reason.isBlank()) ? "고객변심" : reason;
        String url = "https://api.tosspayments.com/v1/payments/" + p.getPaymentKey() + "/cancel";
        String encryptedKey = "Basic " + Base64.getEncoder()
                .encodeToString("test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6:".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", encryptedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String safeReason = cancelReason.replace("\"", "\\\"");
        String jsonBody = String.format("{\"cancelReason\":\"%s\"}", safeReason);

        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class
        );

        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(resp.getBody());

                boolean statusCanceled = "CANCELED".equalsIgnoreCase(root.path("status").asText());
                boolean done =
                        ("DONE".equalsIgnoreCase(root.path("cancelStatus").asText())) ||
                        (root.path("cancels").isArray()
                                && root.path("cancels").size() > 0
                                && "DONE".equalsIgnoreCase(root.path("cancels").get(0).path("cancelStatus").asText()));

                if (statusCanceled && done) {
                    p.setStatus(Payment.Status.CANCELLED);
                    repository.save(p);

                    if (p.getReservationId() != null) {
                        reservationRepository.findById(p.getReservationId()).ifPresent(rv -> {
                            rv.setStatus(Reservation.Status.CANCELLED);
                            reservationRepository.save(rv);
                        });
                    }

                    Map<String, Object> out = new HashMap<>();
                    out.put("message", "cancelled");
                    out.put("paymentId", p.getId());
                    out.put("reason", cancelReason);
                    return ResponseEntity.ok(out);
                }
            } catch (Exception ignore) {}
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "cancel failed", "raw", resp.getBody()));
    }

    /** 하위호환 GET (가능하면 프론트는 POST 사용) */
    @Transactional
    @GetMapping("/cancel/{paymentId}")
    public ResponseEntity<?> cancelPaymentLegacy(
            @PathVariable Long paymentId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return cancelPayment(paymentId, reason);
    }
}
