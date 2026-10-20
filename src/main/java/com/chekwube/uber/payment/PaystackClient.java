package com.chekwube.uber.payment;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Component
public class PaystackClient {
    private final RestClient client;
    private final String secretKey;

    public PaystackClient(RestClient.Builder builder,
                          @Value("${PAYSTACK_SECRET_KEY:}") String secretKey,
                          @Value("${PAYSTACK_BASE_URL:https://api.paystack.co}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
    }

    public Checkout initialize(String email, long amountMinor, String reference, String callbackUrl) {
        requireConfigured();
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email); payload.put("amount", Long.toString(amountMinor));
        payload.put("currency", "NGN"); payload.put("reference", reference);
        if (callbackUrl != null && !callbackUrl.isBlank()) payload.put("callback_url", callbackUrl);
        JsonNode response = execute(() -> client.post().uri("/transaction/initialize")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey).contentType(MediaType.APPLICATION_JSON)
            .body(payload).retrieve().body(JsonNode.class));
        JsonNode data = response == null ? null : response.path("data");
        String authorizationUrl = data == null ? "" : data.path("authorization_url").asText("");
        String accessCode = data == null ? "" : data.path("access_code").asText("");
        if (response == null || !response.path("status").asBoolean(false) || authorizationUrl.isBlank() || accessCode.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack did not return a checkout session");
        return new Checkout(authorizationUrl, accessCode);
    }

    public VerifiedTransaction verify(String reference) {
        requireConfigured();
        JsonNode response = execute(() -> client.get().uri("/transaction/verify/{reference}", reference)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey).retrieve().body(JsonNode.class));
        JsonNode data = response == null ? null : response.path("data");
        if (response == null || !response.path("status").asBoolean(false) || data == null || data.isMissingNode())
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack could not verify this transaction");
        return new VerifiedTransaction(data.path("reference").asText(""), data.path("status").asText(""),
            data.path("currency").asText(""), data.path("amount").asLong(-1),
            data.path("id").canConvertToLong() ? data.path("id").asLong() : null);
    }

    public ProviderOperation refund(String reference, String reason) {
        requireConfigured();
        Map<String, Object> payload = Map.of("transaction", reference, "customer_note", reason, "merchant_note", reason);
        JsonNode response = execute(() -> client.post().uri("/refund")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey).contentType(MediaType.APPLICATION_JSON)
            .body(payload).retrieve().body(JsonNode.class));
        JsonNode data = response == null ? null : response.path("data");
        if (response == null || !response.path("status").asBoolean(false) || data == null || data.isMissingNode())
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack did not accept the refund request");
        return new ProviderOperation(data.path("id").canConvertToLong() ? data.path("id").asLong() : null,
            data.path("status").asText("pending"), data.path("reference").asText(""));
    }

    public ProviderOperation transfer(String recipientCode, long amountMinor, String reference, String reason) {
        requireConfigured();
        Map<String, Object> payload = Map.of("source", "balance", "amount", amountMinor, "recipient", recipientCode,
            "reference", reference, "reason", reason);
        JsonNode response = execute(() -> client.post().uri("/transfer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey).contentType(MediaType.APPLICATION_JSON)
            .body(payload).retrieve().body(JsonNode.class));
        JsonNode data = response == null ? null : response.path("data");
        if (response == null || !response.path("status").asBoolean(false) || data == null || data.isMissingNode())
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack did not accept the transfer");
        return new ProviderOperation(data.path("id").canConvertToLong() ? data.path("id").asLong() : null,
            data.path("status").asText("pending"), data.path("transfer_code").asText(""));
    }

    private <T> T execute(java.util.function.Supplier<T> call) {
        try { return call.get(); }
        catch (RestClientException exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Paystack API request failed"); }
    }
    private void requireConfigured() {
        if (secretKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Paystack payments are not configured");
    }
    public record Checkout(String authorizationUrl, String accessCode) {}
    public record VerifiedTransaction(String reference, String status, String currency, long amountMinor, Long transactionId) {}
    public record ProviderOperation(Long providerId, String status, String code) {}
}
