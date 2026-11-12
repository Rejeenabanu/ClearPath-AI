
package com.clearpath.clearpath_api.service;

import com.clearpath.clearpath_api.entity.PayPalWebhookEvent;
import com.clearpath.clearpath_api.repository.PayPalWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

@Service
public class PayPalWebhookEventStorageService {

    private final PayPalWebhookEventRepository repository;

    public PayPalWebhookEventStorageService(
            PayPalWebhookEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean storeIfNew(JsonNode event) {

        String eventId = event.path("id").asText("");
        String eventType = event.path("event_type").asText("");

        if (eventId.isBlank() || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook event ID and type are required");
        }

        // Prevent duplicate event processing.
        if (repository.existsByEventId(eventId)) {
            return false;
        }

        JsonNode resource = event.path("resource");
        JsonNode amountNode = resource.path("amount");

        String orderId = extractOrderId(eventType, resource);

        String amountText = amountNode.path("value").asText(null);

        BigDecimal amount = null;

        if (amountText != null) {
            try {
                amount = new BigDecimal(amountText);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Invalid amount in webhook event", ex);
            }
        }

        PayPalWebhookEvent webhookEvent =
                new PayPalWebhookEvent(
                        eventId,
                        orderId,
                        eventType,
                        resource.path("id").asText(null),
                        resource.path("status").asText(null),
                        amount,
                        amountNode.path("currency_code").asText(null),
                        event.path("create_time").asText(null)
                );

        repository.save(webhookEvent);

        return true;
    }

    private String extractOrderId(
            String eventType,
            JsonNode resource) {

        if (eventType.startsWith("CHECKOUT.ORDER.")) {
            String id = resource.path("id").asText("");
            return id.isBlank() ? null : id;
        }

        String id = resource
                .path("supplementary_data")
                .path("related_ids")
                .path("order_id")
                .asText("");

        return id.isBlank() ? null : id;
    }
}

