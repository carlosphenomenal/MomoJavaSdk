package io.github.carlosphenomenal;

import java.time.Instant;

public record CachedToken(String token, Instant expiresAt) {
    boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
