package io.github.carlosphenomenal.Dtos;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MomoTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
