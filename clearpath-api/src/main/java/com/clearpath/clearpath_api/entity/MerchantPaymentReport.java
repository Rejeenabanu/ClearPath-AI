
package com.clearpath.clearpath_api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "merchant_payment_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_merchant_paypal_order",
                columnNames = "paypal_order_id"
        )
)
public class MerchantPaymentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paypal_order_id", nullable = false)
    private String paypalOrderId;

    @Column(name = "merchant_order_id", nullable = false)
    private String merchantOrderId;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant reportedAt;

    protected MerchantPaymentReport() {
    }

    public MerchantPaymentReport(
            String paypalOrderId,
            String merchantOrderId,
            String paymentStatus,
            BigDecimal amount,
            String currency) {

        this.paypalOrderId = paypalOrderId;
        this.merchantOrderId = merchantOrderId;
        this.paymentStatus = paymentStatus;
        this.amount = amount;
        this.currency = currency;
        this.reportedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getPaypalOrderId() {
        return paypalOrderId;
    }

    public String getMerchantOrderId() {
        return merchantOrderId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }
}
