package com.clearpath.clearpath_api.controller;

import com.clearpath.clearpath_api.service.PayPalOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/paypal")
public class PayPalOrderController {

    private final PayPalOrderService orderService;

    public PayPalOrderController(
            PayPalOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    public PayPalOrderService.CreatedOrder createOrder() {
        return orderService.createTestOrder();
    }

    @GetMapping("/return")
    public Map<String, String> approvalReturn(
            @RequestParam("token") String orderId) {
        return Map.of(
                "message",
                "Returned from PayPal. Server-side verification "
                        + "and capture are still required.",
                "orderId", orderId
        );
    }

    @GetMapping("/cancel")
    public Map<String, String> cancelOrder(
            @RequestParam(value = "token",
                    required = false) String orderId) {
        return Map.of(
                "message", "PayPal checkout was cancelled.",
                "orderId", orderId == null ? "unknown" : orderId
        );
    }

    @PostMapping("/orders/{orderId}/capture")
    public Map<?, ?> captureOrder(@PathVariable String orderId) {
        return orderService.captureOrder(orderId);
    }

    @GetMapping("/orders/{orderId}/verify")
    public Map<String, String> verifyOrder(
            @PathVariable String orderId) {
        return orderService.getOrderSummary(orderId);
    }
}

