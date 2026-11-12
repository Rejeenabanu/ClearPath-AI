
package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.TransactionTimelineService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clearpath/transactions")
public class TransactionTimelineController {

    private final TransactionTimelineService timelineService;

    public TransactionTimelineController(
            TransactionTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/{orderId}/timeline")
    public TransactionTimelineService.TimelineResponse getTimeline(
            @PathVariable String orderId) {

        if (!orderId.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException(
                    "Invalid PayPal order ID");
        }

        return timelineService.getTimeline(orderId);
    }
}
