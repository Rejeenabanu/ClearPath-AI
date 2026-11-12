package com.clearpath.clearpath_api.service;

import com.clearpath.clearpath_api.config.PayPalProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PayPalOrderService {

    private final PayPalAuthService authService;
    private final RestClient restClient;

    public PayPalOrderService(
            PayPalAuthService authService,
            PayPalProperties properties) {
        this.authService = authService;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    public CreatedOrder createTestOrder() {
        Map<String, Object> experienceContext = Map.of(
                "brand_name", "ClearPath AI",
                "landing_page", "LOGIN",
                "user_action", "PAY_NOW",
                "return_url", "http://localhost:8080/api/paypal/return",
                "cancel_url", "http://localhost:8080/api/paypal/cancel"
        );

        Map<String, Object> requestBody = Map.of(
                "intent", "CAPTURE",
                "payment_source", Map.of(
                        "paypal", Map.of(
                                "experience_context", experienceContext
                        )
                ),
                "purchase_units", List.of(
                        Map.of(
                                "reference_id", "CLEARPATH-DEMO",
                                "description",
                                "ClearPath AI Sandbox test purchase",
                                "amount", Map.of(
                                        "currency_code", "USD",
                                        "value", "10.00"
                                )
                        )
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("/v2/checkout/orders")
                .headers(headers -> {
                    headers.setBearerAuth(
                            authService.getAccessToken());
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set(
                            "PayPal-Request-Id",
                            UUID.randomUUID().toString());
                })
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "PayPal returned an empty response");
        }

        String orderId = String.valueOf(response.get("id"));
        String status = String.valueOf(response.get("status"));
        String approvalUrl = findApprovalUrl(response.get("links"));

        if ("null".equals(orderId)
                || approvalUrl == null) {
            throw new IllegalStateException(
                    "PayPal response did not contain an order ID "
                            + "and approval URL");
        }

        return new CreatedOrder(orderId, status, approvalUrl);
    }

    private String findApprovalUrl(Object linksObject) {
        if (linksObject instanceof List<?> links) {
            // Prefer the current PayPal relationship name.
            for (Object item : links) {
                if (item instanceof Map<?, ?> link
                        && "payer-action".equals(link.get("rel"))) {
                    return String.valueOf(link.get("href"));
                }
            }

            // Keep backward compatibility with older responses.
            for (Object item : links) {
                if (item instanceof Map<?, ?> link
                        && "approve".equals(link.get("rel"))) {
                    return String.valueOf(link.get("href"));
                }
            }
        }

        return null;
    }

    public Map<?, ?> captureOrder(String orderId) {
        if (orderId == null || !orderId.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid PayPal order ID");
        }

        Map<?, ?> response = restClient.post()
                .uri("/v2/checkout/orders/{orderId}/capture", orderId)
                .headers(headers -> {
                    headers.setBearerAuth(authService.getAccessToken());
                    headers.setContentType(
                            org.springframework.http.MediaType.APPLICATION_JSON);
                })
                .body(Map.of())
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException(
                    "PayPal returned an empty capture response");
        }

        return response;
    }


    public Map<String, String> getOrderSummary(String orderId) {
        if (orderId == null || !orderId.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid PayPal order ID");
        }

        Map<?, ?> order = restClient.get()
                .uri("/v2/checkout/orders/{orderId}", orderId)
                .headers(headers ->
                        headers.setBearerAuth(authService.getAccessToken()))
                .retrieve()
                .body(Map.class);

        if (order == null) {
            throw new IllegalStateException(
                    "PayPal returned an empty order response");
        }

        List<?> units = asList(order.get("purchase_units"));
        Map<?, ?> unit = units.isEmpty()
                ? Map.of()
                : asMap(units.get(0));

        Map<?, ?> payments = asMap(unit.get("payments"));
        List<?> captures = asList(payments.get("captures"));

        Map<?, ?> capture = captures.isEmpty()
                ? Map.of()
                : asMap(captures.get(0));

        Map<?, ?> amount = asMap(capture.get("amount"));

        return Map.of(
                "orderId", valueOrDefault(order, "id", "UNKNOWN"),
                "orderStatus", valueOrDefault(order, "status", "UNKNOWN"),
                "captureId", valueOrDefault(capture, "id", "NOT_CAPTURED"),
                "captureStatus", valueOrDefault(
                        capture, "status", "NOT_CAPTURED"),
                "amount", valueOrDefault(amount, "value", "UNKNOWN"),
                "currency", valueOrDefault(
                        amount, "currency_code", "UNKNOWN")
        );
    }



    private Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map
                ? map
                : Map.of();
    }

    private List<?> asList(Object value) {
        return value instanceof List<?> list
                ? list
                : List.of();
    }

    private String valueOrDefault(
            Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null
                ? fallback
                : value.toString();
    }



    public record CreatedOrder(
            String orderId,
            String status,
            String approvalUrl) {
    }
}

