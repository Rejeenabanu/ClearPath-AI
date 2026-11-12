package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.ReconciliationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/clearpath")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
            ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @PostMapping("/reconcile/{orderId}")
    public Map<String, Object> reconcile(
            @PathVariable String orderId,
            @RequestBody ReconciliationRequest request) {

        return reconciliationService.reconcile(
                orderId,
                request.merchantStatus(),
                request.merchantAmount(),
                request.merchantCurrency()
        );
    }

    public record ReconciliationRequest(
            String merchantStatus,
            String merchantAmount,
            String merchantCurrency
    ) {}
}

