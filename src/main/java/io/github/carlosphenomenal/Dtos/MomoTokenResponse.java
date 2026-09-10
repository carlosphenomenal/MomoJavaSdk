package io.github.carlosphenomenal.Dtos;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Represents the response from the MTN MoMo authentication API.
 *
 * @param accessToken the access token to be used for subsequent API calls
 * @param tokenType   the type of the token (e.g., Bearer)
 * @param expiresIn   the number of seconds until the token expires
 * @author Carlos Amanya
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MomoTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
