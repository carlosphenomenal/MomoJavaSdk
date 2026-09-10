package io.github.carlosphenomenal.Products;

import lombok.Getter;

/**
 * Implementation of the MTN MoMo Disbursement product.
 *
 * @author Carlos Amanya
 */
@Getter
public class MomoDisbursement extends MomoProduct {
    public MomoDisbursement(String subscriptionKey) {
        super(subscriptionKey);
        if (subscriptionKey == null || subscriptionKey.isEmpty()) {
            throw new IllegalArgumentException("Subscription key cannot be null or empty");
        }
    }
}
