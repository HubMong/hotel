
package com.example.backend;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository repository;
    private final RestTemplate restTemplate;

    @PostMapping("/add")
    public String add(@RequestBody Payment payment) {

        Payment inputPayment = new Payment();
        inputPayment.setId(payment.getId());
        inputPayment.setOrderId(payment.getOrderId());
        inputPayment.setPaymentKey(payment.getPaymentKey());
        inputPayment.setAmount(payment.getAmount());
        inputPayment.setStatus(payment.getStatus());

        repository.save(inputPayment);

        return "결제 추가 완료";
    }

    @Transactional
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@RequestBody Payment payment) {
        Payment request = repository.findByOrderId(payment.getOrderId());
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "잘못된 주문 ID"));
        }
        if (request.getAmount() != payment.getAmount()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "결제 금액 불일치"));
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
                request.setStatus("completed");
                repository.save(request);
                return ResponseEntity.ok(Map.of("message", "결제 승인 완료"));
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "결제 승인 실패: " + responseBody));
            }
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "결제 승인 실패"));
        }
    }
}
