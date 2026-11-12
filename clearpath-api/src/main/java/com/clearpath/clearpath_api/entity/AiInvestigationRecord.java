
package com.clearpath.clearpath_api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_investigation_reports")
public class AiInvestigationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    private String responseId;

    private String model;

    private String deterministicResult;

    @Column(length = 3000)
    private String executiveSummary;

    @Lob
    @Column(nullable = false)
    private String aiAnalysisJson;

    @Column(nullable = false)
    private Instant createdAt;

    protected AiInvestigationRecord() {
    }

    public AiInvestigationRecord(
            String orderId,
            String responseId,
            String model,
            String deterministicResult,
            String executiveSummary,
            String aiAnalysisJson) {

        this.orderId = orderId;
        this.responseId = responseId;
        this.model = model;
        this.deterministicResult = deterministicResult;
        this.executiveSummary = executiveSummary;
        this.aiAnalysisJson = aiAnalysisJson;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getModel() {
        return model;
    }

    public String getDeterministicResult() {
        return deterministicResult;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public String getAiAnalysisJson() {
        return aiAnalysisJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
