package mk.ukim.finki.aibotbackend.integration.vezilka;

import mk.ukim.finki.aibotbackend.model.enums.DonationStatus;
import mk.ukim.finki.aibotbackend.model.exception.VezilkaIntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP integration client for the doniraj.vezilka.ai Public Donation API.
 *
 * <p>The API accepts two identities (see its documentation):</p>
 * <ul>
 *   <li>{@code X-Donation-Api-Key}: a key issued by the Vezilka administrator,
 *       valid for all endpoints;</li>
 *   <li>{@code Authorization: Bearer}: the access token of a logged-in Vezilka
 *       user, valid for text donations only (and awarding points).</li>
 * </ul>
 * <p>When {@code VEZILKA_ACCESS_TOKEN} is set it is used; otherwise the API key.</p>
 */
@Component
public class VezilkaHttpClient implements VezilkaClient {
    private static final String API_KEY_HEADER = "X-Donation-Api-Key";

    private final RestClient client;
    private final String apiKey;
    private final String accessToken;
    private final String submitPath;
    private final String statusPath;

    public VezilkaHttpClient(
        VezilkaProperties vezilkaProperties,
        @Value("${vezilka.access-token:}") String accessToken,
        @Value("${vezilka.submit-path:/api/public/v1/donations/text/}") String submitPath,
        @Value("${vezilka.status-path:/api/public/v1/donations/{reference}/}") String statusPath
    ) {
        this.apiKey = vezilkaProperties.apiKey() == null ? "" : vezilkaProperties.apiKey().trim();
        this.accessToken = accessToken == null ? "" : accessToken.trim();
        this.submitPath = submitPath;
        this.statusPath = statusPath;
        this.client = RestClient.builder().baseUrl(vezilkaProperties.baseUrl()).build();
    }

    private void authenticate(HttpHeaders headers) {
        if (!accessToken.isBlank()) {
            headers.setBearerAuth(accessToken);
        } else {
            headers.set(API_KEY_HEADER, apiKey);
        }
    }

    @Override
    public DonationReceipt submitTextDonation(TextDonationRequest request) {
        try {
            VezilkaDonationResponse response = client.post()
                .uri(submitPath)
                .headers(this::authenticate)
                .body(VezilkaDonationEnvelope.ofOne(request))
                .retrieve()
                .body(VezilkaDonationResponse.class);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                throw new VezilkaIntegrationException("Vezilka returned no result for the submitted item");
            }
            return DonationReceipt.from(response.results().getFirst());
        } catch (VezilkaIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new VezilkaIntegrationException("Donation submission failed", exception);
        }
    }

    @Override
    public DonationStatus checkStatus(String vezilkaReference) {
        // The stored reference is the first item id, optionally followed by
        // " (+N)" for the rest of the batch; the first id is enough to check.
        String firstReference = vezilkaReference == null
            ? null
            : vezilkaReference.split("[ ,]", 2)[0].trim();
        try {
            VezilkaDonationResult result = client.get()
                .uri(statusPath, firstReference)
                .headers(this::authenticate)
                .retrieve()
                .body(VezilkaDonationResult.class);
            if (result == null) {
                return null;
            }
            return switch (result.status() == null ? "" : result.status().toLowerCase()) {
                case "accepted" -> DonationStatus.ACCEPTED;
                case "rejected" -> DonationStatus.REJECTED;
                default -> null;
            };
        } catch (Exception exception) {
            throw new VezilkaIntegrationException("Donation status check failed", exception);
        }
    }
}
