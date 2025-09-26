package com.example.backend.payment;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository repository;
    private final RestTemplate restTemplate;

    @GetMapping("/{id}")
    public ResponseEntity<Payment> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(payment -> {
                    System.out.println("[API] Found payment: " + payment);
                    return ResponseEntity.ok(payment);
                })
                .orElseGet(() -> {
                    System.out.println("[API] No payment found for id: " + id);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                });
    }

    @Transactional
    @PostMapping("/add")
    public ResponseEntity<Payment> add(@RequestBody Payment payment) {
        payment.setStatus("PENDING");
        repository.save(payment);
        return ResponseEntity.ok(payment);
    }

    @Transactional
    @PostMapping("/confirm")
    public ResponseEntity<Payment> confirm(@RequestBody Payment payment) {
        Payment request = repository.findByOrderId(payment.getOrderId());
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (request.getAmount() != payment.getAmount()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        request.setPaymentKey(payment.getPaymentKey());

        String tossApprovalUrl = "https://api.tosspayments.com/v1/payments/confirm";
        String encryptedKey = "Basic "
                + Base64.getEncoder().encodeToString("test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6:".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", encryptedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format("{\"orderId\": \"%s\", \"amount\": %d, \"paymentKey\": \"%s\"}",
                request.getOrderId(), request.getAmount(), request.getPaymentKey());

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // Toss 결제 승인 요청
        ResponseEntity<String> response = restTemplate.exchange(tossApprovalUrl, HttpMethod.POST, entity, String.class);

        System.out.println(response.toString());

        if (response.getStatusCode() == HttpStatus.OK) {
            String responseBody = response.getBody();
            if (responseBody != null && responseBody.contains("\"status\":\"DONE\"")) {
                // 결제 승인 성공
                request.setStatus("COMPLETED");
                repository.save(request);
                return ResponseEntity.ok(request);
            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("lists")
    public ResponseEntity<List<Payment>> getLists() {

        List<Payment> lists = new ArrayList<Payment>();
        lists = repository.findAll();
        return ResponseEntity.ok(lists);
    }

    @Transactional
    @GetMapping("view/{paymentId}")
    public ResponseEntity<?> viewPayment(@PathVariable Long paymentId) {
        Optional<Payment> optionalRequest = repository.findById(paymentId);

        // 이 부분에 get 요청자와 결제 진행자가 동일한지 or 관리자의 요청인지 확인해야함

        if (optionalRequest.isEmpty()) {
            // 결제 내역 없을 시 예외
        }

        Payment request = optionalRequest.get();

        String tossViewUrl = "https://api.tosspayments.com/v1/payments/" + request.getPaymentKey();
        String encryptedKey = "Basic "
                + Base64.getEncoder().encodeToString("test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6:".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", encryptedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Toss 내역 확인 요청
        ResponseEntity<String> response = restTemplate.exchange(tossViewUrl, HttpMethod.GET, entity, String.class);
        ObjectMapper mapper = new ObjectMapper();

        try {
            TossPaymentResponse tossPayment = mapper.readValue(response.getBody(), TossPaymentResponse.class);

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", tossPayment.getOrderId());
            result.put("orderName", tossPayment.getOrderName());
            result.put("requestedAt", tossPayment.getRequestedAt());
            result.put("approvedAt", tossPayment.getApprovedAt());
            result.put("amount", tossPayment.getTotalAmount());
            result.put("status", tossPayment.getStatus());
            result.put("receiptUrl", tossPayment.getReceipt() != null ? tossPayment.getReceipt().getUrl() : null);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "응답 파싱 실패"));
        }
    }

    @Transactional
    @GetMapping("cancel/{paymentId}")
    public ResponseEntity<Map<String, String>> cancelPayment(@PathVariable Long paymentId) {
        Optional<Payment> optionalRequest = repository.findById(paymentId);

        // 이 부분에 get 요청자와 결제 진행자가 동일한지 or 관리자의 요청인지 확인해야함

        if (optionalRequest.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "해당 결제 정보를 찾을 수 없습니다."));
        }

        Payment request = optionalRequest.get();

        if (request.getStatus().equals("CANCELED")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "이미 취소된 내역입니다."));
        }

        String tossCancelUrl = "https://api.tosspayments.com/v1/payments/" + request.getPaymentKey() + "/cancel";
        String encryptedKey = "Basic "
                + Base64.getEncoder().encodeToString("test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6:".getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", encryptedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = "{\"cancelReason\": \"고객변심\"}";

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // Toss 결제 취소 요청
        ResponseEntity<String> response = restTemplate.exchange(tossCancelUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String responseBody = response.getBody();

            if (responseBody != null) {
                if (responseBody.contains("\"status\":\"CANCELED\"")
                        && responseBody.contains("\"cancelStatus\":\"DONE\"")) {
                    // 결제 취소 성공
                    request.setStatus("CANCELED");
                    repository.save(request);
                    return ResponseEntity.ok(Map.of("message", "결제 취소 완료"));
                } else {
                    // 상태는 OK지만 응답 내용이 이상한 경우
                    return ResponseEntity.status(HttpStatus.OK).body(
                            Map.of("message", "결제 취소 실패 (응답 파싱 실패)", "response", responseBody));
                }
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        Map.of("message", "결제 취소 실패: 응답 본문이 null"));
            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "결제 취소 실패: HTTP 상태 오류"));
        }
    }

}
