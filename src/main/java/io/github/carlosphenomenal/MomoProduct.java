package io.github.carlosphenomenal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class MomoProduct {
    private final String subscriptionKey;
}
