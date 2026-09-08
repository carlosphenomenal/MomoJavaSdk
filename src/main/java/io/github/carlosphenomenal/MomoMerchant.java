package io.github.carlosphenomenal;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Builder
public class MomoMerchant {
    private final TargetEnvironment targetEnvironment;
    private final String apiUser;
    private final String apiKey;
    private final Set<MomoProduct> products;

    private final Map<MomoProduct, CachedToken> tokenCache = new ConcurrentHashMap<>();
    static HttpClient httpClient = HttpClient.newHttpClient();
    static ObjectMapper objectMapper = new ObjectMapper();

    public static class MomoMerchantBuilder {

        public MomoMerchant build() {

            Objects.requireNonNull(targetEnvironment, "Target environment must not be null");

            if (targetEnvironment != TargetEnvironment.SANDBOX) {
                if (apiUser == null || apiUser.isBlank()) {
                    throw new IllegalArgumentException("An apiUser is required for non-sandbox environments");
                }
                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalArgumentException("An apiKey is required for non-sandbox environments");
                }
            }

            if (products == null || products.isEmpty()) {
                throw new IllegalArgumentException("Products set must not be null or empty");
            }

            if (targetEnvironment == TargetEnvironment.SANDBOX && apiUser == null) {
                String subscriptionKey = products.iterator().next().getSubscriptionKey();
                this.apiUser = createApiUser(subscriptionKey);
            }

            if (targetEnvironment == TargetEnvironment.SANDBOX && apiKey == null) {
                String subscriptionKey = products.iterator().next().getSubscriptionKey();
                this.apiKey = generateApiKey(this.apiUser, subscriptionKey);
            }

            return new MomoMerchant(targetEnvironment, apiUser, apiKey, products);
        }
    }

    public String getAccessToken(MomoProduct product) {
        CachedToken existing = tokenCache.get(product);
        if (existing != null && !existing.isExpired()) {
            return existing.token();
        }

        MomoTokenResponse response = fetchNewToken(product);

        Instant refreshAt = Instant.now().plusSeconds(Math.max(0, response.expiresIn() - 60));
        CachedToken newToken = new CachedToken(response.accessToken(), refreshAt);

        tokenCache.put(product, newToken);
        return response.accessToken();
    }

    public String requestClientToPay(PaymentRequest paymentRequest, String referenceId) {
        RequestToPayBody requestToPayBody = RequestToPayBody.builder()
                .amount(paymentRequest.getAmount())
                .currency(this.targetEnvironment.getCurrency())
                .externalId(paymentRequest.getExternalId())
                .payer(new PaymentRequest.Payer(paymentRequest.getPayer().getPartyIdType(), this.resolvePartyId(paymentRequest.getPayer())))
                .payerMessage(paymentRequest.getPayerMessage())
                .payeeNote(paymentRequest.getPayeeNote())
                .build();

        Collections collections = products.stream()
                .filter(p -> p instanceof Collections)
                .findFirst()
                .map(p -> (Collections) p)
                .orElseThrow(() -> new IllegalArgumentException("No Collections product available"));
        return collections.requestToPay(this, httpClient, objectMapper, requestToPayBody, referenceId);
    }

    private String resolvePartyId(PaymentRequest.Payer payer) {
        return switch (payer.getPartyIdType()) {
            case MSISDN -> resolveMsisdn(payer.getPartyId());
            case EMAIL -> resolveEmail(payer.getPartyId());
            case PARTY_CODE -> resolvePartyCode(payer.getPartyId());
            case null -> throw new IllegalArgumentException("PartyIdType cannot be null.");
        };
    }

    private String resolveMsisdn(String payerNumber) {
        if (this.targetEnvironment.getCountryCode() == null) return payerNumber;

        if (payerNumber == null || payerNumber.isBlank()) {
            throw new IllegalArgumentException("Payer id cannot be null or blank");
        }

        // Remove spaces, hyphens, parentheses, etc.
        String number = payerNumber.replaceAll("[^0-9+]", "");

        // Remove leading +
        if (number.startsWith("+")) {
            number = number.substring(1);
        }

        // Already in international format
        if (number.startsWith(this.targetEnvironment.getCountryCode())) {
            if (number.length() != 12) {
                throw new IllegalArgumentException(
                        "Invalid " + this.targetEnvironment.name() + " MSISDN: " + payerNumber
                );
            }

            return number;
        }

        // Local format: 07XXXXXXXX or 03XXXXXXXX
        if (number.startsWith("0")) {
            number = number.substring(1);
        }

        // At this point we expect 9 digits, e.g. 775035028
        if (number.length() != 9) {
            throw new IllegalArgumentException(
                    "Invalid phone number: " + payerNumber
            );
        }

        return this.targetEnvironment.getCountryCode() + number;
    }

    private String resolveEmail(String email) {
        // TODO: Implement email resolution
        return email;
    }

    private String resolvePartyCode(String partyCode){
        // TODO: Implement party code resolution
        return partyCode;
    }

    private MomoTokenResponse fetchNewToken(MomoProduct product) {
        String contextPath;
        if (product instanceof Disbursement) {
            contextPath = "disbursement";
        } else if (product instanceof Collections) {
            contextPath = "collection";
        } else {
            throw new IllegalArgumentException("Unsupported product type: " + product.getClass().getSimpleName());
        }

        String url = this.targetEnvironment.getBaseUrl() + "/" + contextPath + "/token/";

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))

                    .header("Authorization", "Basic " + basicAuth(this.apiUser, this.apiKey))
                    .header("Ocp-Apim-Subscription-Key", product.getSubscriptionKey())
                    .header("Content-Type", "application/json")

                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {

                throw new RuntimeException(
                        "Failed to retrieve MoMo access token for "
                                + product
                                + ". HTTP status: "
                                + statusCode
                                + ". Response: "
                                + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    MomoTokenResponse.class
            );

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException(
                    "Failed to retrieve MoMo access token for "
                            + product
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    private static String createApiUser(String subscriptionKey) {
        String url = "https://sandbox.momodeveloper.mtn.com/v1_0/apiuser";
        String referenceId = java.util.UUID.randomUUID().toString();

        try {
            String jsonBody = objectMapper.writeValueAsString(
                    Map.of("providerCallbackHost", "example.com")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Reference-Id", referenceId)
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Failed to create Sandbox API User. Status: "
                        + response.statusCode() + ", Body: " + response.body());
            }
            return referenceId;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Error creating Sandbox API User: " + e.getMessage(), e);
        }
    }

    private static String generateApiKey(String apiUser, String subscriptionKey) {
        String url = "https://sandbox.momodeveloper.mtn.com/v1_0/apiuser/" + apiUser + "/apikey";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("Failed to generate Sandbox API Key. Status: "
                        + response.statusCode() + ", Body: " + response.body());
            }

            ApiKeyResponse keyResponse = objectMapper.readValue(response.body(), ApiKeyResponse.class);
            return keyResponse.apiKey();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Error generating Sandbox API Key: " + e.getMessage(), e);
        }
    }

    private static String basicAuth(String user, String key) {
        return Base64.getEncoder().encodeToString((user + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private record ApiKeyResponse(String apiKey) {}

}
