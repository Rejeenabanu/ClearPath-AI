
package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.EvidenceReconciliationService;
import com.clearpath.clearpath_api.service.EvidenceReconciliationService.InvestigationResponse;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clearpath/investigations")
public class EvidenceReconciliationController {

    private final EvidenceReconciliationService investigationService;

    public EvidenceReconciliationController(
            EvidenceReconciliationService investigationService) {
        this.investigationService = investigationService;
    }

    @GetMapping("/{orderId}")
    public InvestigationResponse investigate(
            @PathVariable String orderId) {

        return investigationService.investigate(orderId);
    }
}
