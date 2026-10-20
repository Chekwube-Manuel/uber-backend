package com.chekwube.uber.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class PaymentAdminController {
    private final PaymentOperationsService operations;
    public PaymentAdminController(PaymentOperationsService operations) { this.operations = operations; }

    @PostMapping("/payments/{reference}/refund")
    public PaymentOperationsService.RefundResponse refund(@PathVariable String reference,
                                                          @Valid @RequestBody RefundRequest request) {
        return operations.refund(reference, request.reason());
    }

    @PostMapping("/rides/{rideId}/payout")
    public PaymentOperationsService.PayoutResponse payout(@PathVariable UUID rideId,
                                                          @Valid @RequestBody PayoutRequest request) {
        return operations.payout(rideId, request.recipientCode());
    }

    public record RefundRequest(@NotBlank @Size(max = 500) String reason) {}
    public record PayoutRequest(@NotBlank @Size(max = 100) String recipientCode) {}
}
