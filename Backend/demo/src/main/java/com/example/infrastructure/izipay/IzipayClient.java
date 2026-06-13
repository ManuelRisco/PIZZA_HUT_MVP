package com.example.infrastructure.izipay;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class IzipayClient {

    @Value("${izipay.username}")
    private String username;

    @Value("${izipay.password}")
    private String password;

    @Value("${izipay.apiUrl}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public IzipayClient() {
        this.restTemplate = new RestTemplate();
    }

    public String createPaymentToken(double amount, String currency, String orderId, String customerEmail) {
        String url = apiUrl + "/api-payment/V4/Charge/CreatePayment";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, Object> body = new HashMap<>();
        // Izipay expects amount in smallest currency unit (e.g. cents). For 10.50 PEN -> 1050
        long amountInCents = Math.round(amount * 100);
        body.put("amount", amountInCents);
        body.put("currency", currency); // "PEN"
        body.put("orderId", orderId);
        
        Map<String, String> customer = new HashMap<>();
        customer.put("email", customerEmail);
        body.put("customer", customer);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            org.springframework.core.ParameterizedTypeReference<Map<String, Object>> typeRef = 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {};
            @SuppressWarnings("null")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, request, typeRef);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                String status = (String) responseBody.get("status");
                if ("SUCCESS".equals(status) && responseBody.containsKey("answer")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> answer = (Map<String, Object>) responseBody.get("answer");
                    return (String) answer.get("formToken");
                } else if ("ERROR".equals(status) && responseBody.containsKey("answer")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> answer = (Map<String, Object>) responseBody.get("answer");
                    throw new RuntimeException("Error Izipay: " + answer.get("errorMessage"));
                }
            }
            throw new RuntimeException("Error inesperado en respuesta de Izipay: " + responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con Izipay: " + e.getMessage(), e);
        }
    }
}
