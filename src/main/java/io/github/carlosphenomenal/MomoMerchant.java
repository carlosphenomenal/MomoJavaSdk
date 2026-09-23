package io.github.carlosphenomenal;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.carlosphenomenal.Dtos.*;
import io.github.carlosphenomenal.Enums.TargetEnvironment;
import io.github.carlosphenomenal.Products.MomoCollections;
import io.github.carlosphenomenal.Products.MomoDisbursement;
import io.github.carlosphenomenal.Products.MomoProduct;
import io.github.carlosphenomenal.Provisioners.AccessTokenProvisioner;
import io.github.carlosphenomenal.Provisioners.SandboxProvisioner;
import io.github.carlosphenomenal.Types.TypeUniqueSet;
import lombok.Builder;
import lombok.Getter;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The primary entry point for the MTN MoMo SDK.
 * This class handles authentication, token caching, and provides methods to interact with various MoMo products.
 *
 * @author Carlos Amanya
 */
@Getter
@Builder
public class MomoMerchant {

    private final TargetEnvironment targetEnvironment;
    private final String apiUser;
    private final String apiKey;
    private final TypeUniqueSet<MomoProduct> products;

    private final Map<MomoProduct, CachedToken> tokenCache = new ConcurrentHashMap<>();
    static HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Builder for {@link MomoMerchant}.
     */
    public static class MomoMerchantBuilder {

        /**
         * Builds a {@link MomoMerchant} instance.
         * For {@link TargetEnvironment#SANDBOX}, if the apiKey is not provided, it will be automatically provisioned.
         * The apiUser must always be provided.
         *
         * @return a new {@link MomoMerchant} instance
         * @throws IllegalArgumentException if required fields are missing
         */
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

            if (targetEnvironment == TargetEnvironment.SANDBOX && apiKey == null) {
                String subscriptionKey = products.iterator().next().getSubscriptionKey();
                this.apiKey = SandboxProvisioner.generateApiKey(httpClient, apiUser, subscriptionKey);
            }

            return new MomoMerchant(targetEnvironment, apiUser, apiKey, products);
        }
    }

    /**
     * Gets a valid access token for the specified MoMo product.
     * Uses a cached token if it's still valid, otherwise fetches a new one.
     *
     * @param product the MoMo product to get the token for
     * @return a valid access token
     */
    public String getAccessToken(MomoProduct product) {

        CachedToken existing = tokenCache.get(product);

        if (existing != null && !existing.isExpired()) {
            return existing.token();
        }

        synchronized (tokenCache) {

            existing = tokenCache.get(product);

            if (existing != null && !existing.isExpired()) {
                return existing.token();
            }

            MomoTokenResponse response = AccessTokenProvisioner.fetchNewToken(httpClient, product, targetEnvironment, apiUser, apiKey);

            Instant refreshAt = Instant.now().plusSeconds(Math.max(0, response.expiresIn() - 60));

            CachedToken newToken = new CachedToken(response.accessToken(), refreshAt);

            tokenCache.put(product, newToken);

            return newToken.token();
        }
    }

    /**
     * Initiates a request to pay from a client.
     *
     * @param paymentRequest the payment request details
     * @param referenceId    a unique UUID for this transaction
     * @return the referenceId if successful
     * @throws IllegalArgumentException if the Collections product is not available
     */
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

    /**
     * Checks the status of a payment request.
     *
     * @param referenceId the unique UUID of the transaction to check
     * @return the {@link PaymentStatus} of the transaction
     * @throws IllegalArgumentException if the Collections product is not available
     */
    public PaymentStatus checkPaymentStatus(String referenceId) {
        MomoCollections momoCollections = products.get(MomoCollections.class);
        if (momoCollections == null) {
            throw new IllegalArgumentException("No Collections product available");
        }
        return momoCollections.checkPaymentStatus(this, httpClient, objectMapper, referenceId);
    }

    /**
     * Initiates a refund for a previous transaction.
     *
     * @param refundRequest the refund details
     * @param referenceId   a unique UUID for this refund transaction
     * @return the referenceId if successful
     * @throws IllegalArgumentException if the Disbursement product is not available
     */
    public String refund(RefundRequest refundRequest, String referenceId) {
        RefundBody refundBody = RefundBody.builder()
                .amount(refundRequest.getAmount())
                .currency(this.targetEnvironment.getCurrency())
                .externalId(refundRequest.getExternalId())
                .payerMessage(refundRequest.getPayerMessage())
                .payeeNote(refundRequest.getPayeeNote())
                .referenceIdToRefund(refundRequest.getReferenceIdToRefund())
                .build();

        MomoDisbursement momoDisbursement = products.get(MomoDisbursement.class);
        if (momoDisbursement == null) {
            throw new IllegalArgumentException("No Disbursement product available");
        }
        return momoDisbursement.refund(this, httpClient, objectMapper, refundBody, referenceId);
    }

    /**
     * Checks the status of a refund request.
     *
     * @param referenceId the unique UUID of the refund transaction to check
     * @return the {@link RefundStatus} of the refund transaction
     * @throws IllegalArgumentException if the Disbursement product is not available
     */
    public RefundStatus checkRefundStatus(String referenceId) {
        MomoDisbursement momoDisbursement = products.get(MomoDisbursement.class);
        if (momoDisbursement == null) {
            throw new IllegalArgumentException("No Disbursement product available");
        }
        return momoDisbursement.checkRefundStatus(this, httpClient, objectMapper, referenceId);
    }

    /**
     * Initiates a transfer to a payee.
     *
     * @param transferRequest the transfer details
     * @param referenceId     a unique UUID for this transfer transaction
     * @return the referenceId if successful
     * @throws IllegalArgumentException if the Disbursement product is not available
     */
    public String transfer(TransferRequest transferRequest, String referenceId) {
        TransferBody transferBody = TransferBody.builder()
                .amount(transferRequest.getAmount())
                .currency(this.targetEnvironment.getCurrency())
                .externalId(transferRequest.getExternalId())
                .payee(new TransferBody.Payee(transferRequest.getPayee().getPartyIdType(), this.resolveTransferPartyId(transferRequest.getPayee())))
                .payerMessage(transferRequest.getPayerMessage())
                .payeeNote(transferRequest.getPayeeNote())
                .build();

        MomoDisbursement momoDisbursement = products.get(MomoDisbursement.class);
        if (momoDisbursement == null) {
            throw new IllegalArgumentException("No Disbursement product available");
        }
        return momoDisbursement.transfer(this, httpClient, objectMapper, transferBody, referenceId);
    }

    /**
     * Checks the status of a transfer request.
     *
     * @param referenceId the unique UUID of the transfer transaction to check
     * @return the {@link TransferStatus} of the transfer transaction
     * @throws IllegalArgumentException if the Disbursement product is not available
     */
    public TransferStatus checkTransferStatus(String referenceId) {
        MomoDisbursement momoDisbursement = products.get(MomoDisbursement.class);
        if (momoDisbursement == null) {
            throw new IllegalArgumentException("No Disbursement product available");
        }
        return momoDisbursement.checkTransferStatus(this, httpClient, objectMapper, referenceId);
    }

    /**
     * Gets the account balance for the Collections product.
     *
     * @return the {@link BalanceResponse}
     * @throws IllegalArgumentException if the Collections product is not available
     */
    public BalanceResponse getAccountBalance() {
        return getAccountBalance(MomoCollections.class);
    }

    /**
     * Gets the account balance for a specific product.
     *
     * @param productClass the class of the MoMo product to get the balance for
     * @param <T>          the type of the MoMo product
     * @return the {@link BalanceResponse}
     * @throws IllegalArgumentException if the product is not available or does not support balance checks
     */
    public <T extends MomoProduct> BalanceResponse getAccountBalance(Class<T> productClass) {
        T product = products.get(productClass);
        if (product == null) {
            throw new IllegalArgumentException("Product " + productClass.getSimpleName() + " not available");
        }
        return product.getAccountBalance(this, httpClient, objectMapper);
    }


    //================ HELPER METHODS  ======================

    private String resolveTransferPartyId(TransferRequest.Payee payee) {
        return switch (payee.getPartyIdType()) {
            case MSISDN -> resolveMsisdn(payee.getPartyId());
            case EMAIL -> resolveEmail(payee.getPartyId());
            case PARTY_CODE -> resolvePartyCode(payee.getPartyId());
            case null -> throw new IllegalArgumentException("PartyIdType cannot be null.");
        };
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


    //================= Records ===========================
    public record CachedToken(String token, Instant expiresAt) {
        boolean isExpired() {
            return !Instant.now().isAfter(expiresAt);
        }
    }




}
