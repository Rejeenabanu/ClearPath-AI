
package com.clearpath.clearpath_api.service;

import com.clearpath.clearpath_api.entity.PayPalWebhookEvent;
import com.clearpath.clearpath_api.repository.PayPalWebhookEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionTimelineService {

    private final PayPalWebhookEventRepository repository;

    public TransactionTimelineService(
            PayPalWebhookEventRepository repository) {
        this.repository = repository;
    }

    public TimelineResponse getTimeline(String orderId) {

        List<PayPalWebhookEvent> events =
                repository.findByOrderIdOrderByReceivedAtAsc(orderId);

        List<TimelineEvent> timeline = events.stream()
                .map(event -> new TimelineEvent(
                        event.getEventId(),
                        event.getEventType(),
                        event.getResourceId(),
                        event.getResourceStatus(),
                        event.getAmount(),
                        event.getCurrency(),
                        event.getPaypalEventTime(),
                        event.getReceivedAt().toString()
                ))
                .toList();

        return new TimelineResponse(
                orderId,
                timeline.size(),
                timeline
        );
    }

    public record TimelineResponse(
            String orderId,
            int eventCount,
            List<TimelineEvent> events) {}

    public record TimelineEvent(
            String eventId,
            String eventType,
            String resourceId,
            String resourceStatus,
            java.math.BigDecimal amount,
            String currency,
            String paypalEventTime,
            String receivedAt) {}
}

