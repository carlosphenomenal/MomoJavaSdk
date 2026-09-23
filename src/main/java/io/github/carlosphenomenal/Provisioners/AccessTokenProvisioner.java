package io.github.carlosphenomenal.Provisioners;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.carlosphenomenal.Dtos.MomoTokenResponse;
import io.github.carlosphenomenal.Enums.TargetEnvironment;
import io.github.carlosphenomenal.Products.MomoCollections;
import io.github.carlosphenomenal.Products.MomoDisbursement;
import io.github.carlosphenomenal.Products.MomoProduct;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class AccessTokenProvisioner {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_ATTEMPTS = 3;

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(30);


    public static MomoTokenResponse fetchNewToken(
            HttpClient httpClient,
            MomoProduct product,
            TargetEnvironment targetEnvironment,
            String apiUser,
            String apiKey
    ) {

        String contextPath = resolveContextPath(product);

        String url = targetEnvironment.getBaseUrl() + "/" + contextPath + "/token/";

        validateUrl(url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Basic " + basicAuth(apiUser, apiKey))
                .header("Ocp-Apim-Subscription-Key", product.getSubscriptionKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();


        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {

                HttpResponse<String> response =
                        httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                int statusCode = response.statusCode();

                /*
                 * Successful token response.
                 */
                if (statusCode >= 200 && statusCode < 300) {

                    return objectMapper.readValue(
                            response.body(),
                            MomoTokenResponse.class
                    );
                }


                /*
                 * Do NOT retry authentication or request errors.
                 */
                if (!isRetryableStatus(statusCode)) {

                    throw new RuntimeException(
                            "Failed to retrieve MoMo access token for "
                                    + product.getClass().getSimpleName()
                                    + ". HTTP status: "
                                    + statusCode
                                    + ". Response: "
                                    + response.body()
                    );
                }


                /*
                 * Server-side temporary error.
                 */
                if (attempt < MAX_ATTEMPTS) {

                    sleepBeforeRetry(attempt);
                }

            } catch (IOException e) {

                lastException = e;

                if (attempt == MAX_ATTEMPTS) {
                    break;
                }

                sleepBeforeRetry(attempt);
            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new RuntimeException(
                        "Thread interrupted while retrieving "
                                + "MoMo access token",
                        e
                );

            }
        }


        throw new RuntimeException(
                "Failed to retrieve MoMo access token for "
                        + product.getClass().getSimpleName()
                        + " after "
                        + MAX_ATTEMPTS
                        + " attempts. URL: "
                        + url
                        + ". Last error: "
                        + (lastException == null
                        ? "unknown"
                        : lastException),
                lastException
        );
    }


    private static String resolveContextPath(MomoProduct product) {

        if (product instanceof MomoDisbursement) {
            return "disbursement";
        }

        if (product instanceof MomoCollections) {
            return "collection";
        }

        throw new IllegalArgumentException(
                "Unsupported product type: "
                        + product.getClass().getSimpleName()
        );
    }


    private static boolean isRetryableStatus(int statusCode) {

        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504;
    }


    private static void sleepBeforeRetry(int attempt) {

        try {

            long delayMillis =
                    (long) Math.pow(2, attempt - 1) * 500;

            Thread.sleep(delayMillis);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Interrupted while waiting to retry MoMo request",
                    e
            );
        }
    }


    private static void validateUrl(String url) {

        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid MoMo API URL: " + url,
                    e
            );
        }

        if (uri.getHost() == null) {

            throw new IllegalArgumentException(
                    "MoMo API URL has no valid host: " + url
            );
        }
    }


    private static String basicAuth(
            String user,
            String key
    ) {

        String credentials = user + ":" + key;

        return Base64.getEncoder().encodeToString( credentials.getBytes(StandardCharsets.UTF_8));
    }
}