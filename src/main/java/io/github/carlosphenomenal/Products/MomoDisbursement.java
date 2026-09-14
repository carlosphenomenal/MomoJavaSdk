package io.github.carlosphenomenal.Products;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.carlosphenomenal.Dtos.RefundBody;
import io.github.carlosphenomenal.Dtos.RefundStatus;
import io.github.carlosphenomenal.MomoMerchant;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

/**
 * Implementation of the MTN MoMo Disbursement product.
 *
 * @author Carlos Amanya
 */
@Getter
@Slf4j
public class MomoDisbursement extends MomoProduct {
    public MomoDisbursement(String subscriptionKey) {
        super(subscriptionKey);
        if (subscriptionKey == null || subscriptionKey.isEmpty()) {
            throw new IllegalArgumentException("Subscription key cannot be null or empty");
        }
    }

    /**
     * Initiates a refund for a previous transaction.
     *
     * @param momoMerchant   the MomoMerchant instance
     * @param httpClient     the HttpClient to use
     * @param objectMapper   the ObjectMapper for JSON serialization
     * @param refundBody     the refund details
     * @param referenceId    the unique reference ID for this refund request
     * @return the reference ID of the refund
     */
    public String refund(MomoMerchant momoMerchant, HttpClient httpClient, ObjectMapper objectMapper, RefundBody refundBody, String referenceId) {
        try {
            String accessToken = momoMerchant.getAccessToken(this);
            String url = momoMerchant.getTargetEnvironment().getBaseUrl() + "/disbursement/v1_0/refund";
            String jsonPayload = objectMapper.writeValueAsString(refundBody);

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
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 202) {
                log.info("[MoMo][REFUND] Initiated successfully. RefundRef={}", referenceId);
                return referenceId;
            } else {
                throw new RuntimeException("Refund failed with HTTP status: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Refund failed", e);
        }
    }

    /**
     * Checks the status of a refund.
     *
     * @param momoMerchant  the MomoMerchant instance
     * @param httpClient    the HttpClient to use
     * @param objectMapper  the ObjectMapper for JSON deserialization
     * @param referenceId   the reference ID of the refund to check
     * @return the {@link RefundStatus} of the refund
     */
    public RefundStatus checkRefundStatus(MomoMerchant momoMerchant, HttpClient httpClient, ObjectMapper objectMapper, String referenceId) {
        try {
            String accessToken = momoMerchant.getAccessToken(this);
            String url = momoMerchant.getTargetEnvironment().getBaseUrl() + "/disbursement/v1_0/refund/" + referenceId;

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
                return objectMapper.readValue(response.body(), RefundStatus.class);
            } else {
                throw new RuntimeException("Check refund status failed with HTTP status: " + response.statusCode() + ", Body: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check refund status", e);
        }
    }
}
