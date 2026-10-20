package com.chekwube.uber.payment;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rider")
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/rides/{rideId}/payment")
    public PaymentDtos.CheckoutResponse initialize(Authentication authentication, @PathVariable UUID rideId) {
        return payments.initialize((UUID) authentication.getPrincipal(), rideId);
    }

    @GetMapping("/payments/{reference}")
    public PaymentDtos.PaymentResponse verify(Authentication authentication, @PathVariable String reference) {
        return payments.verify((UUID) authentication.getPrincipal(), reference);
    }
}