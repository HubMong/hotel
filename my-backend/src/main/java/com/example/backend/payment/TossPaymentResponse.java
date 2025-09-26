package com.example.backend.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossPaymentResponse {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("orderName")
    private String orderName;

    @JsonProperty("requestedAt")
    private String requestedAt;

    @JsonProperty("approvedAt")
    private String approvedAt;

    @JsonProperty("totalAmount")
    private Integer totalAmount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("receipt")
    private Receipt receipt;

    @Data
    public static class Receipt {
        @JsonProperty("url")
        private String url;

    }

}
