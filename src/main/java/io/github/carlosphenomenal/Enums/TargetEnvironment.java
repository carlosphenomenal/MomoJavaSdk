package io.github.carlosphenomenal.Enums;

import lombok.Getter;

/**
 * Enumeration of supported target environments for the MTN MoMo API.
 * Each environment defines its base URL, environment code, currency, and country code.
 *
 * @author Carlos Amanya
 */
@Getter
public enum TargetEnvironment {
    SANDBOX("sandbox", "https://sandbox.momodeveloper.mtn.com", "EUR", null),
    MTN_UGANDA("mtnuganda", "https://proxy.momoapi.mtn.com", "UGX", "256");

    private final String baseUrl;
    private final String environmentCode;
    private final String currency;
    private final String countryCode;


    TargetEnvironment(String environmentCode, String baseUrl, String currency, String countryCode) {
        this.environmentCode = environmentCode;
        this.baseUrl = baseUrl;
        this.currency = currency;
        this.countryCode = countryCode;
    }

}
