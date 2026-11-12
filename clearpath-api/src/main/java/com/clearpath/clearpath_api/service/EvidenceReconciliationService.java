
package com.clearpath.clearpath_api.service;

import com.clearpath.clearpath_api.entity.MerchantPaymentReport;
import com.clearpath.clearpath_api.entity.PayPalWebhookEvent;
import com.clearpath.clearpath_api.repository.MerchantPaymentReportRepository;
import com.clearpath.clearpath_api.repository.PayPalWebhookEventRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class EvidenceReconciliationService {

    private final PayPalWebhookEventRepository eventRepository;
    private final MerchantPaymentReportRepository merchantRepository;

    public EvidenceReconciliationService(
            PayPalWebhookEventRepository eventRepository,
            MerchantPaymentReportRepository merchantRepository) {

        this.eventRepository = eventRepository;
        this.merchantRepository = merchantRepository;
    }

    public InvestigationResponse investigate(String orderId) {

        if (orderId == null || !orderId.matches("[A-Z0-9]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid PayPal order ID");
        }

        // 1. Retrieve verified PayPal events.
        List<PayPalWebhookEvent> events =
                eventRepository.findByOrderIdOrderByReceivedAtAsc(orderId);

        if (events.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No PayPal evidence found for this order");
        }

        // 2. Retrieve merchant evidence.
        MerchantPaymentReport merchant =
                merchantRepository.findByPaypalOrderId(orderId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Merchant report not found for this order"));

        // 3. Find the latest successfully completed capture.
        PayPalWebhookEvent capture = events.stream()
                .filter(event ->
                        "PAYMENT.CAPTURE.COMPLETED"
                                .equals(event.getEventType()))
                .filter(event ->
                        "COMPLETED".equalsIgnoreCase(
                                event.getResourceStatus()))
                .max(Comparator.comparing(
                        PayPalWebhookEvent::getReceivedAt))
                .orElse(null);

        MerchantEvidence merchantEvidence =
                new MerchantEvidence(
                        merchant.getMerchantOrderId(),
                        merchant.getPaymentStatus(),
                        merchant.getAmount(),
                        merchant.getCurrency(),
                        merchant.getReportedAt().toString()
                );

        // 4. Handle orders without a completed capture.
        if (capture == null) {

            String latestStatus = events.stream()
                    .filter(event -> event.getEventType()
                            .startsWith("PAYMENT.CAPTURE."))
                    .max(Comparator.comparing(
                            PayPalWebhookEvent::getReceivedAt))
                    .map(PayPalWebhookEvent::getResourceStatus)
                    .orElse("NO_CAPTURE_EVENT");

            return new InvestigationResponse(
                    orderId,
                    "REVIEW",
                    "A successfully completed PayPal capture "
                            + "has not been verified.",
                    null,
                    merchantEvidence,
                    List.of(
                            "Latest available capture status: "
                                    + latestStatus,
                            "Do not assume that the payment has completed."
                    ),
                    "Check the latest PayPal order and capture status "
                            + "before changing the merchant payment record."
            );
        }

        // 5. Compare PayPal and merchant evidence.
        List<String> findings = new java.util.ArrayList<>();

        boolean statusMismatch =
                !"COMPLETED".equalsIgnoreCase(
                        merchant.getPaymentStatus());

        boolean amountMismatch =
                capture.getAmount() == null
                        || capture.getAmount().compareTo(
                        merchant.getAmount()) != 0;

        boolean currencyMismatch =
                capture.getCurrency() == null
                        || !capture.getCurrency().equalsIgnoreCase(
                        merchant.getCurrency());

        if (statusMismatch) {
            findings.add(
                    "PayPal confirms a completed capture, but the "
                            + "merchant reports "
                            + merchant.getPaymentStatus() + "."
            );
        }

        if (amountMismatch) {
            findings.add(
                    "The merchant-reported amount does not match "
                            + "the PayPal captured amount."
            );
        }

        if (currencyMismatch) {
            findings.add(
                    "The merchant-reported currency does not match "
                            + "the PayPal captured currency."
            );
        }

        boolean mismatch =
                statusMismatch || amountMismatch || currencyMismatch;

        String result = mismatch ? "MISMATCH" : "MATCH";

        if (!mismatch) {
            findings.add(
                    "Payment status, amount and currency agree "
                            + "across both records."
            );
        }

        String recommendation = mismatch
                ? "Investigate merchant transaction mapping, webhook "
                  + "delivery and internal payment processing logs. "
                  + "Verify the original payment before attempting "
                  + "another charge or refund."
                : "No discrepancy detected in the supplied evidence. "
                  + "Continue normal transaction monitoring.";

        // 6. Return the evidence-backed investigation.
        PayPalEvidence paypalEvidence = new PayPalEvidence(
                capture.getEventId(),
                capture.getResourceId(),
                capture.getResourceStatus(),
                capture.getAmount(),
                capture.getCurrency(),
                capture.getReceivedAt().toString()
        );

        return new InvestigationResponse(
                orderId,
                result,
                mismatch
                        ? "Discrepancy detected between PayPal "
                          + "and merchant records."
                        : "PayPal and merchant records are consistent.",
                paypalEvidence,
                merchantEvidence,
                findings,
                recommendation
        );
    }

    public record PayPalEvidence(
            String eventId,
            String captureId,
            String captureStatus,
            BigDecimal amount,
            String currency,
            String receivedAt
    ) {}

    public record MerchantEvidence(
            String merchantOrderId,
            String paymentStatus,
            BigDecimal amount,
            String currency,
            String reportedAt
    ) {}

    public record InvestigationResponse(
            String orderId,
            String result,
            String summary,
            PayPalEvidence paypalEvidence,
            MerchantEvidence merchantEvidence,
            List<String> findings,
            String recommendedAction
    ) {}
}
