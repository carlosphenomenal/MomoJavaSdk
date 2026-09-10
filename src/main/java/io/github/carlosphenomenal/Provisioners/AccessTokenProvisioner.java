package io.github.carlosphenomenal.Provisioners;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.carlosphenomenal.Dtos.MomoTokenResponse;
import io.github.carlosphenomenal.Products.MomoCollections;
import io.github.carlosphenomenal.Products.MomoDisbursement;
import io.github.carlosphenomenal.Products.MomoProduct;
import io.github.carlosphenomenal.Enums.TargetEnvironment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Internal class for provisioning access tokens from the MTN MoMo API.
 *
 * @author Carlos Amanya
 */
public class AccessTokenProvisioner {

    static ObjectMapper objectMapper = new ObjectMapper();

    public static MomoTokenResponse fetchNewToken(HttpClient httpClient, MomoProduct product, TargetEnvironment targetEnvironment, String apiUser, String apiKey) {
        String contextPath;
        if (product instanceof MomoDisbursement) {
            contextPath = "disbursement";
        } else if (product instanceof MomoCollections) {
            contextPath = "collection";
        } else {
            throw new IllegalArgumentException("Unsupported product type: " + product.getClass().getSimpleName());
        }

        String url = targetEnvironment.getBaseUrl() + "/" + contextPath + "/token/";

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))

                    .header("Authorization", "Basic " + basicAuth(apiUser, apiKey))
                    .header("Ocp-Apim-Subscription-Key", product.getSubscriptionKey())
                    .header("Content-Type", "application/json")

                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            int statusCode = response.statusCode();

            if (statusCode < 200 || statusCode >= 300) {

                throw new RuntimeException(
                        "Failed to retrieve MoMo access token for "
                                + product
                                + ". HTTP status: "
                                + statusCode
                                + ". Response: "
                                + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    MomoTokenResponse.class
            );

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new RuntimeException(
                    "Failed to retrieve MoMo access token for "
                            + product
                            + ": "
                            + e.getMessage(),
                    e
            );
        }
    }

    private static String basicAuth(String user, String key) {
        return Base64.getEncoder().encodeToString((user + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

}
