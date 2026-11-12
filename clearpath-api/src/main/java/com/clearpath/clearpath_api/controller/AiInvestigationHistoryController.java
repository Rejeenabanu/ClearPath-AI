
package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.entity.AiInvestigationRecord;
import com.clearpath.clearpath_api.repository.AiInvestigationRecordRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/clearpath/investigations")
public class AiInvestigationHistoryController {

    private final AiInvestigationRecordRepository repository;
    private final String expectedToken;

    public AiInvestigationHistoryController(
            AiInvestigationRecordRepository repository,
            @Value("${clearpath.ai-access-token}") String expectedToken) {

        this.repository = repository;
        this.expectedToken = expectedToken;
    }

    @GetMapping("/{orderId}/ai/history")
    public List<InvestigationHistoryItem> getHistory(
            @PathVariable String orderId,
            @RequestHeader(
                    value = "X-ClearPath-Token",
                    required = false
            ) String providedToken) {

        if (orderId == null || !orderId.matches("[A-Z0-9]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid order ID");
        }

        if (!isValidToken(providedToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        return repository
                .findTop20ByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(record -> new InvestigationHistoryItem(
                        record.getId(),
                        record.getOrderId(),
                        record.getModel(),
                        record.getDeterministicResult(),
                        record.getExecutiveSummary(),
                        record.getAiAnalysisJson(),
                        record.getCreatedAt().toString()
                ))
                .toList();
    }

    private boolean isValidToken(String providedToken) {

        if (expectedToken == null || expectedToken.isBlank()
                || providedToken == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    public record InvestigationHistoryItem(
            Long reportId,
            String orderId,
            String model,
            String result,
            String executiveSummary,
            String aiAnalysisJson,
            String createdAt
    ) {}
}
