package io.github.carlosphenomenal.Products;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.carlosphenomenal.Dtos.BalanceResponse;
import io.github.carlosphenomenal.MomoMerchant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.http.HttpClient;

/**
 * Base class for all MTN MoMo products.
 *
 * @author Carlos Amanya
 */
@Getter
@RequiredArgsConstructor
public abstract class MomoProduct {
    private final String subscriptionKey;

    public BalanceResponse getAccountBalance(MomoMerchant momoMerchant, HttpClient httpClient, ObjectMapper objectMapper) {
        throw new UnsupportedOperationException("Account balance is not supported for " + this.getClass().getSimpleName());
    }
}
