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
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Builder
public class MomoMerchant {

    private final TargetEnvironment targetEnvironment;
    private final String apiUser;
    private final String apiKey;
    private final TypeUniqueSet<MomoProduct> products;

    private final Map<MomoProduct, CachedToken> tokenCache = new ConcurrentHashMap<>();
    static HttpClient httpClient = HttpClient.newHttpClient();
    static ObjectMapper objectMapper = new ObjectMapper();

    public static class MomoMerchantBuilder {

        public MomoMerchant build() {

            Objects.requireNonNull(targetEnvironment, "Target environment must not be null");
            Objects.requireNonNull(apiUser, "API user must not be null");

            if (targetEnvironment != TargetEnvironment.SANDBOX) {

                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalArgumentException("An apiKey is required for non-sandbox environments");
                }
            }

            if (products == null || products.isEmpty()) {
                throw new IllegalArgumentException("Products set must not be null or empty");
            }

            if (targetEnvironment == TargetEnvironment.SANDBOX && apiUser == null) {
                String subscriptionKey = products.iterator().next().getSubscriptionKey();
                this.apiUser = SandboxProvisioner.createApiUser(httpClient, subscriptionKey);
            }

            if (targetEnvironment == TargetEnvironment.SANDBOX && apiKey == null) {
                String subscriptionKey = products.iterator().next().getSubscriptionKey();
                this.apiKey = SandboxProvisioner.generateApiKey(httpClient, apiUser, subscriptionKey);
            }

            return new MomoMerchant(targetEnvironment, apiUser, apiKey, products);
        }
    }

    public String getAccessToken(MomoProduct product) {
        CachedToken existing = tokenCache.get(product);
        if (existing != null && !existing.isExpired()) {
            return existing.token();
        }

        MomoTokenResponse response = AccessTokenProvisioner.fetchNewToken(httpClient, product, targetEnvironment, apiUser, apiKey);

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

        MomoCollections momoCollections = products.get(MomoCollections.class);
        if (momoCollections == null) {
            throw new IllegalArgumentException("No Collections product available");
        }
        return momoCollections.requestToPay(this, httpClient, objectMapper, requestToPayBody, referenceId);
    }

    public PaymentStatus checkPaymentStatus(String referenceId) {
        MomoCollections momoCollections = products.get(MomoCollections.class);
        if (momoCollections == null) {
            throw new IllegalArgumentException("No Collections product available");
        }
        return momoCollections.checkPaymentStatus(this, httpClient, objectMapper, referenceId);
    }


    //================ HELPER METHODS  ======================

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




}
