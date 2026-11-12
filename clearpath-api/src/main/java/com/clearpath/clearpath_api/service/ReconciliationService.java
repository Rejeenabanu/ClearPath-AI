package com.clearpath.clearpath_api.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReconciliationService {

    private final PayPalOrderService payPalOrderService;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "COMPLETED", "PENDING", "NOT_RECEIVED", "FAILED"
    );

    public ReconciliationService(
            PayPalOrderService payPalOrderService) {
        this.payPalOrderService = payPalOrderService;
    }

    public Map<String, Object> reconcile(
            String orderId,
            String merchantStatus,
            String merchantAmount,
            String merchantCurrency) {

        if (merchantStatus == null
                || merchantAmount == null
                || merchantCurrency == null) {
            throw new IllegalArgumentException(
                    "Merchant status, amount and currency are required");
        }

        String status = merchantStatus.trim()
                .toUpperCase(Locale.ROOT);
        String currency = merchantCurrency.trim()
                .toUpperCase(Locale.ROOT);

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Unsupported merchant payment status");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(merchantAmount);
            if (amount.signum() < 0) {
                throw new IllegalArgumentException(
                        "Merchant amount cannot be negative");
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Merchant amount must be a valid number");
        }

        // Fetch current evidence directly from PayPal.
        Map<String, String> paypal =
                payPalOrderService.getOrderSummary(orderId);

        String paypalOrderStatus = paypal.get("orderStatus");
        String paypalCaptureStatus = paypal.get("captureStatus");
        String paypalAmount = paypal.get("amount");
        String paypalCurrency = paypal.get("currency");

        List<String> findings = new ArrayList<>();

        boolean statusMismatch =
                "COMPLETED".equals(paypalCaptureStatus)
                        && !"COMPLETED".equals(status);

        if (statusMismatch) {
            findings.add(
                    "PayPal reports a completed capture, but the "
                            + "merchant reports " + status + ".");
        }

        if ("COMPLETED".equals(status)
                && !"COMPLETED".equals(paypalCaptureStatus)) {
            statusMismatch = true;
            findings.add(
                    "The merchant reports a completed payment, "
                            + "but PayPal does not report a completed capture.");
        }

        boolean amountMismatch =
                isKnown(paypalAmount)
                        && new BigDecimal(paypalAmount).compareTo(amount) != 0;

        if (amountMismatch) {
            findings.add(
                    "The merchant amount differs from PayPal's captured amount.");
        }

        boolean currencyMismatch =
                isKnown(paypalCurrency)
                        && !paypalCurrency.equalsIgnoreCase(currency);

        if (currencyMismatch) {
            findings.add(
                    "The merchant currency differs from PayPal's currency.");
        }

        boolean mismatch =
                statusMismatch || amountMismatch || currencyMismatch;

        String result;
        String recommendation;

        if (mismatch) {
            result = "MISMATCH";
            recommendation =
                    "Check merchant transaction mapping, webhook delivery "
                            + "and processing logs. Verify the original payment "
                            + "before attempting another charge or refund.";
        } else if ("COMPLETED".equals(paypalCaptureStatus)
                && "COMPLETED".equals(status)) {
            result = "MATCH";
            findings.add(
                    "Payment statuses agree. Amount and currency were checked.");
            recommendation =
                    "No discrepancy detected in the supplied records.";
        } else {
            result = "REVIEW";
            findings.add(
                    "The payment is not confirmed as completed in both systems.");
            recommendation =
                    "Retrieve the latest transaction states and investigate "
                            + "before taking further action.";
        }

        return Map.of(
                "orderId", orderId,
                "result", result,
                "paypalEvidence", Map.of(
                        "orderStatus", safe(paypalOrderStatus),
                        "captureStatus", safe(paypalCaptureStatus),
                        "amount", safe(paypalAmount),
                        "currency", safe(paypalCurrency)
                ),
                "merchantReport", Map.of(
                        "paymentStatus", status,
                        "amount", merchantAmount,
                        "currency", currency
                ),
                "findings", findings,
                "recommendedAction", recommendation
        );
    }

    private boolean isKnown(String value) {
        return value != null
                && !value.isBlank()
                && !"UNKNOWN".equalsIgnoreCase(value)
                && !"NOT_CAPTURED".equalsIgnoreCase(value);
    }

    private String safe(String value) {
        return value == null ? "UNKNOWN" : value;
    }
}

