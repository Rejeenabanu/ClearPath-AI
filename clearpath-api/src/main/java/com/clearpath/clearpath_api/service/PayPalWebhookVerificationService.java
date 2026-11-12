package com.clearpath.clearpath_api.service;

import com.clearpath.clearpath_api.config.PayPalProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PayPalWebhookVerificationService {

    private final PayPalAuthService authService;
    private final PayPalProperties properties;
    private final RestClient restClient;

    public PayPalWebhookVerificationService(
            PayPalAuthService authService,
            PayPalProperties properties) {
        this.authService = authService;
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    public boolean verify(HttpHeaders headers, JsonNode event) {
        String webhookId = properties.webhookId();

        if (webhookId == null || webhookId.isBlank()) {
            throw new IllegalStateException(
                    "PAYPAL_WEBHOOK_ID is not configured");
        }

        String authAlgo = headers.getFirst("PAYPAL-AUTH-ALGO");
        String certUrl = headers.getFirst("PAYPAL-CERT-URL");
        String transmissionId =
                headers.getFirst("PAYPAL-TRANSMISSION-ID");
        String transmissionSig =
                headers.getFirst("PAYPAL-TRANSMISSION-SIG");
        String transmissionTime =
                headers.getFirst("PAYPAL-TRANSMISSION-TIME");

        if (isBlank(authAlgo)
                || isBlank(certUrl)
                || isBlank(transmissionId)
                || isBlank(transmissionSig)
                || isBlank(transmissionTime)) {
            return false;
        }

        Map<String, Object> verificationRequest =
                new LinkedHashMap<>();

        verificationRequest.put("auth_algo", authAlgo);
        verificationRequest.put("cert_url", certUrl);
        verificationRequest.put("transmission_id", transmissionId);
        verificationRequest.put(
                "transmission_sig", transmissionSig);
        verificationRequest.put(
                "transmission_time", transmissionTime);
        verificationRequest.put("webhook_id", webhookId);
        verificationRequest.put("webhook_event", event);

        Map<?, ?> response = restClient.post()
                .uri("/v1/notifications/verify-webhook-signature")
                .headers(requestHeaders -> {
                    requestHeaders.setBearerAuth(
                            authService.getAccessToken());
                    requestHeaders.setContentType(
                            MediaType.APPLICATION_JSON);
                })
                .body(verificationRequest)
                .retrieve()
                .body(Map.class);

        return response != null
                && "SUCCESS".equalsIgnoreCase(
                String.valueOf(
                        response.get("verification_status")));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

