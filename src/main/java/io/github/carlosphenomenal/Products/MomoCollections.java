package io.github.carlosphenomenal.Products;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.carlosphenomenal.MomoMerchant;
import io.github.carlosphenomenal.Dtos.PaymentStatus;
import io.github.carlosphenomenal.Dtos.RequestToPayBody;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.UUID;

@Getter
@Slf4j
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

            if (referenceId == null) {
                referenceId = UUID.randomUUID().toString();
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Target-Environment", momoMerchant.getTargetEnvironment().getEnvironmentCode())
                    .header("Ocp-Apim-Subscription-Key", this.getSubscriptionKey())
                    .header("X-Reference-Id", referenceId)
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

    public PaymentStatus checkPaymentStatus(MomoMerchant momoMerchant, HttpClient httpClient, ObjectMapper objectMapper, String referenceId) {
        try {
            String accessToken = momoMerchant.getAccessToken(this);
            String url = momoMerchant.getTargetEnvironment().getBaseUrl() + "/collection/v1_0/requesttopay/" + referenceId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Target-Environment", momoMerchant.getTargetEnvironment().getEnvironmentCode())
                    .header("Ocp-Apim-Subscription-Key", this.getSubscriptionKey())
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Payment request initiated successfully.");
                return objectMapper.readValue(response.body(), PaymentStatus.class);
            } else {
                throw new RuntimeException("Check payment status failed with HTTP status: " + response.statusCode() + ", Body: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check payment status", e);
        }
    }
}
