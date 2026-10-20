package com.chekwube.uber.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaystackWebhookControllerTest {
    private static final String SECRET = "test-paystack-secret";

    @Test
    void acceptsSignedSuccessfulChargeAndDelegatesValidatedAmounts() throws Exception {
        PaymentService payments = mock(PaymentService.class);
        PaystackWebhookController controller = new PaystackWebhookController(payments, mock(PaymentOperationsService.class), new ObjectMapper(), SECRET);
        String payload = """
            {"event":"charge.success","data":{"reference":"ridepay-abc123","status":"success","currency":"NGN","amount":70250,"id":123456}}
            """;
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);

        var response = controller.receive(sign(body), body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(payments).handleSuccessfulWebhook("ridepay-abc123", 70250L, "NGN", 123456L);
    }

    @Test
    void rejectsInvalidSignatureBeforeProcessingPayload() {
        PaymentService payments = mock(PaymentService.class);
        PaystackWebhookController controller = new PaystackWebhookController(payments, new ObjectMapper(), SECRET);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> controller.receive("invalid-signature", "{}".getBytes(StandardCharsets.UTF_8)));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(payments);
    }

    private String sign(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}