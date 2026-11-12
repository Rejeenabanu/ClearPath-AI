package com.clearpath.clearpath_api.repository;

import com.clearpath.clearpath_api.entity.PayPalWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayPalWebhookEventRepository
        extends JpaRepository<PayPalWebhookEvent, Long> {

    boolean existsByEventId(String eventId);

    List<PayPalWebhookEvent>
    findByOrderIdOrderByReceivedAtAsc(String orderId);
}
