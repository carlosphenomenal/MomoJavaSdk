package io.github.carlosphenomenal;

import lombok.Getter;

@Getter
public class Disbursement extends MomoProduct {
    public Disbursement(String subscriptionKey) {
        super(subscriptionKey);
        if (subscriptionKey == null || subscriptionKey.isEmpty()) {
            throw new IllegalArgumentException("Subscription key cannot be null or empty");
        }
    }
}
