package io.github.carlosphenomenal;

import lombok.Getter;

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
