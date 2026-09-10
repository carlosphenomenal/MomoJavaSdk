package io.github.carlosphenomenal;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
