package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.PayPalAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PayPalAuthController {

    private final PayPalAuthService authService;

    public PayPalAuthController(PayPalAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/paypal/health")
    public Map<String, String> testAuthentication() {
        authService.getAccessToken();

        return Map.of(
                "paypal", "connected",
                "environment", "sandbox",
                "authentication", "successful"
        );
    }
}
