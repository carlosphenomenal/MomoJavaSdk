package io.github.carlosphenomenal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Getter
public class MomoCollections extends MomoProduct {
    public MomoCollections(String subscriptionKey) {
        super(subscriptionKey);
        if (subscriptionKey == null || subscriptionKey.isEmpty()) {
            throw new IllegalArgumentException("Subscription key cannot be null or empty");
        }
    }

    public String requestToPay(MomoMerchant momoMerchant, HttpClient httpClient, ObjectMapper objectMapper, RequestToPayBody paymentRequest, String referenceId) {
        try {
            String accessToken = momoMerchant.getAccessToken(this);
            String url = momoMerchant.getTargetEnvironment().getBaseUrl() + "/collection/v1_0/requesttopay";
            String jsonPayload = objectMapper.writeValueAsString(paymentRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Target-Environment", momoMerchant.getTargetEnvironment().getEnvironmentCode())
                    .header("Ocp-Apim-Subscription-Key", this.getSubscriptionKey())
                    .header("X-Reference-Id", referenceId != null ? referenceId : java.util.UUID.randomUUID().toString())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    // .header("X-Callback-Url", callbackBaseUrl + "/momo/callback/payment")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Execute request synchronously
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            // MTN MoMo returns 202 ACCEPTED for successful payment requests
            if (response.statusCode() == 202) {
                return referenceId;
            } else {
                throw new RuntimeException("Request to Pay failed with HTTP status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Request to Pay failed", e);
        }
    }
}
