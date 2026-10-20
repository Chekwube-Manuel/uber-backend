package com.chekwube.uber.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/webhooks/paystack")
public class PaystackWebhookController {
    private final PaymentService payments;
    private final ObjectMapper objectMapper;
    private final String secretKey;

    public PaystackWebhookController(PaymentService payments, ObjectMapper objectMapper,
                                     @Value("${PAYSTACK_SECRET_KEY:}") String secretKey) {
        this.payments = payments;
        this.objectMapper = objectMapper;
        this.secretKey = secretKey == null ? "" : secretKey.trim();
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestHeader(value = "x-paystack-signature", required = false) String signature,
                                        @RequestBody byte[] rawBody) {
        if (secretKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Paystack webhook is not configured");
        if (!isValidSignature(rawBody, signature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Paystack webhook signature");
        }
        try {
            JsonNode event = objectMapper.readTree(rawBody);
            if (!"charge.success".equals(event.path("event").asText())) return ResponseEntity.ok().build();
            JsonNode data = event.path("data");
            String reference = data.path("reference").asText("");
            String currency = data.path("currency").asText("");
            long amount = data.path("amount").asLong(-1);
            Long transactionId = data.path("id").canConvertToLong() ? data.path("id").asLong() : null;
            if (reference.isBlank() || amount < 0 || currency.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed Paystack charge.success event");
            }
            payments.handleSuccessfulWebhook(reference, amount, currency, transactionId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Paystack webhook payload");
        }
    }

    private boolean isValidSignature(byte[] rawBody, String suppliedSignature) {
        if (suppliedSignature == null || suppliedSignature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] expected = mac.doFinal(rawBody);
            byte[] supplied = HexFormat.of().parseHex(suppliedSignature);
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException | java.security.GeneralSecurityException exception) {
            return false;
        }
    }
}