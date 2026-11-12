
package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.PayPalWebhookVerificationService;
import com.clearpath.clearpath_api.service.PayPalWebhookEventStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@RestController
@RequestMapping("/api/paypal/webhooks")
public class PayPalWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(PayPalWebhookController.class);

    private final PayPalWebhookVerificationService verificationService;
    private final PayPalWebhookEventStorageService storageService;

    public PayPalWebhookController(
            PayPalWebhookVerificationService verificationService,
            PayPalWebhookEventStorageService storageService) {

        this.verificationService = verificationService;
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @RequestHeader HttpHeaders headers,
            @RequestBody JsonNode event) {

        String eventId = event.path("id").asText("");
        String eventType = event.path("event_type").asText("");

        if (eventId.isBlank() || eventType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "received", false,
                    "message", "Event ID and event type are required"
            ));
        }

        final boolean verified;

        try {
            verified = verificationService.verify(headers, event);

        } catch (IllegalStateException ex) {
            log.error("Webhook verification is not configured");

            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "received", false,
                    "message", "Webhook verification is not configured"
            ));

        } catch (RestClientException ex) {
            log.error("PayPal webhook verification unavailable");

            return ResponseEntity.status(
                    HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "received", false,
                    "message", "Webhook verification temporarily unavailable"
            ));
        }

        if (!verified) {
            log.warn("Rejected unverified event: {}", eventId);

            return ResponseEntity.status(
                    HttpStatus.UNAUTHORIZED).body(Map.of(
                    "received", false,
                    "message", "Signature verification failed"
            ));
        }

        boolean stored = storageService.storeIfNew(event);

        log.info(
                "Verified PayPal event: id={}, type={}, stored={}",
                eventId, eventType, stored
        );

        return ResponseEntity.ok(Map.of(
                "received", true,
                "verified", true,
                "stored", stored,
                "duplicate", !stored,
                "eventId", eventId,
                "eventType", eventType
        ));
    }
}
