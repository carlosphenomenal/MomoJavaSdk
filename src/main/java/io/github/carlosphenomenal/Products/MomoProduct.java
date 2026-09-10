package io.github.carlosphenomenal.Products;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Base class for all MTN MoMo products.
 *
 * @author Carlos Amanya
 */
@Getter
@RequiredArgsConstructor
public abstract class MomoProduct {
    private final String subscriptionKey;
}
