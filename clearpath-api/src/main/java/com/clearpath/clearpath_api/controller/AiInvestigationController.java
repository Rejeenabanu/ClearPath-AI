
package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.AiInvestigationService;
import com.clearpath.clearpath_api.service.AiInvestigationService.AiInvestigationResult;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/clearpath/investigations")
public class AiInvestigationController {

    private final AiInvestigationService aiInvestigationService;
    private final String expectedToken;

    public AiInvestigationController(
            AiInvestigationService aiInvestigationService,
            @Value("${clearpath.ai-access-token}")
            String expectedToken) {

        this.aiInvestigationService = aiInvestigationService;
        this.expectedToken = expectedToken;
    }

    @PostMapping("/{orderId}/ai")
    public AiInvestigationResult investigate(
            @PathVariable String orderId,
            @RequestHeader(
                    value = "X-ClearPath-Token",
                    required = false
            ) String providedToken) {

        if (!isValidToken(providedToken)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid ClearPath access token"
            );
        }

        return aiInvestigationService.investigate(orderId);
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
}
