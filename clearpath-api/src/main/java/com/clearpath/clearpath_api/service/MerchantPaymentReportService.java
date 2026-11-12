
package com.clearpath.clearpath_api.service;

import com.clearpath.clearpath_api.entity.MerchantPaymentReport;
import com.clearpath.clearpath_api.repository.MerchantPaymentReportRepository;
import com.clearpath.clearpath_api.repository.PayPalWebhookEventRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

@Service
public class MerchantPaymentReportService {

    private final MerchantPaymentReportRepository reportRepository;
    private final PayPalWebhookEventRepository eventRepository;

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "COMPLETED",
            "NOT_RECEIVED",
            "PENDING",
            "FAILED"
    );

    public MerchantPaymentReportService(
            MerchantPaymentReportRepository reportRepository,
            PayPalWebhookEventRepository eventRepository) {

        this.reportRepository = reportRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public MerchantReportResponse saveReport(
            String paypalOrderId,
            String merchantOrderId,
            String paymentStatus,
            BigDecimal amount,
            String currency) {

        if (paypalOrderId == null ||
                !paypalOrderId.matches("[A-Z0-9]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid PayPal order ID");
        }

        if (merchantOrderId == null || merchantOrderId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Merchant order ID is required");
        }

        String status = paymentStatus == null
                ? ""
                : paymentStatus.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported merchant payment status");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A positive payment amount is required");
        }

        String normalizedCurrency = currency == null
                ? ""
                : currency.trim().toUpperCase(Locale.ROOT);

        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid currency code");
        }

        // Ensure ClearPath has received evidence for this PayPal order.
        boolean orderExists = eventRepository
                .findByOrderIdOrderByReceivedAtAsc(paypalOrderId)
                .stream()
                .anyMatch(event ->
                        "CHECKOUT.ORDER.APPROVED".equals(event.getEventType())
                                || "PAYMENT.CAPTURE.COMPLETED"
                                .equals(event.getEventType()));

        if (!orderExists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No verified PayPal order events found");
        }

        if (reportRepository.findByPaypalOrderId(paypalOrderId)
                .isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Merchant report already exists for this order");
        }

        MerchantPaymentReport report =
                new MerchantPaymentReport(
                        paypalOrderId,
                        merchantOrderId,
                        status,
                        amount,
                        normalizedCurrency
                );

        MerchantPaymentReport saved =
                reportRepository.save(report);

        return new MerchantReportResponse(
                saved.getId(),
                saved.getPaypalOrderId(),
                saved.getMerchantOrderId(),
                saved.getPaymentStatus(),
                saved.getAmount(),
                saved.getCurrency(),
                "SAVED"
        );
    }

    public record MerchantReportResponse(
            Long reportId,
            String paypalOrderId,
            String merchantOrderId,
            String paymentStatus,
            BigDecimal amount,
            String currency,
            String result
    ) {}
}
