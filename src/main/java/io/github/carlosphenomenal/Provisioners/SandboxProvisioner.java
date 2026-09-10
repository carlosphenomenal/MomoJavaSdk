package io.github.carlosphenomenal.Provisioners;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class SandboxProvisioner {

    static ObjectMapper objectMapper = new ObjectMapper();

    public static String createApiUser(HttpClient httpClient, String subscriptionKey) {
        String url = "https://sandbox.momodeveloper.mtn.com/v1_0/apiuser";
        String referenceId = java.util.UUID.randomUUID().toString();

        try {
            String jsonBody = objectMapper.writeValueAsString(
                    Map.of("providerCallbackHost", "example.com")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Reference-Id", referenceId)
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new RuntimeException("Failed to create Sandbox API User. Status: "
                        + response.statusCode() + ", Body: " + response.body());
            }
            return referenceId;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Error creating Sandbox API User: " + e.getMessage(), e);
        }
    }

    public static String generateApiKey(HttpClient httpClient, String apiUser, String subscriptionKey) {
        String url = "https://sandbox.momodeveloper.mtn.com/v1_0/apiuser/" + apiUser + "/apikey";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new RuntimeException("Failed to generate Sandbox API Key. Status: "
                        + response.statusCode() + ", Body: " + response.body());
            }

            ApiKeyResponse keyResponse = objectMapper.readValue(response.body(), ApiKeyResponse.class);
            return keyResponse.apiKey();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Error generating Sandbox API Key: " + e.getMessage(), e);
        }
    }


    private record ApiKeyResponse(String apiKey) {}


}
