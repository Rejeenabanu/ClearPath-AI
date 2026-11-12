
package com.clearpath.clearpath_api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "paypal_webhook_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_paypal_webhook_event_id",
                columnNames = "event_id"
        )
)
public class PayPalWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "order_id", length = 20)
    private String orderId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    private String resourceId;

    private String resourceStatus;

    private BigDecimal amount;

    private String currency;

    private String paypalEventTime;

    @Column(nullable = false)
    private Instant receivedAt;

    protected PayPalWebhookEvent() {
    }

    public PayPalWebhookEvent(
            String eventId,
            String orderId,
            String eventType,
            String resourceId,
            String resourceStatus,
            BigDecimal amount,
            String currency,
            String paypalEventTime) {

        this.eventId = eventId;
        this.orderId = orderId;
        this.eventType = eventType;
        this.resourceId = resourceId;
        this.resourceStatus = resourceStatus;
        this.amount = amount;
        this.currency = currency;
        this.paypalEventTime = paypalEventTime;
        this.receivedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceStatus() {
        return resourceStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaypalEventTime() {
        return paypalEventTime;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getOrderId() {
        return orderId;
    }
}

