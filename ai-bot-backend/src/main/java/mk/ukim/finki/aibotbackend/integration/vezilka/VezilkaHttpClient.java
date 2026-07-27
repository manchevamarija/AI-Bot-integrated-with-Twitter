package mk.ukim.finki.aibotbackend.integration.vezilka;

import mk.ukim.finki.aibotbackend.model.enums.DonationStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import mk.ukim.finki.aibotbackend.model.exception.VezilkaIntegrationException;
import java.util.Map;

/**
 * HTTP integration client for doniraj.vezilka.ai.
 */
@Component
public class VezilkaHttpClient implements VezilkaClient {
    private final RestClient client; private final String apiKey;
    private final String submitPath;
    private final String statusPath;
    private final VezilkaProperties vezilkaProperties;

    public VezilkaHttpClient(
        VezilkaProperties vezilkaProperties,
        @Value("${vezilka.submit-path:/api/donations/text}") String submitPath,
        @Value("${vezilka.status-path:/api/donations/{reference}}") String statusPath
    ) {
        this.vezilkaProperties = vezilkaProperties;
        this.submitPath = submitPath;
        this.statusPath = statusPath;
        this.apiKey=vezilkaProperties.apiKey(); this.client=RestClient.builder().baseUrl(vezilkaProperties.baseUrl()).build();
    }

    @Override
    public DonationReceipt submitTextDonation(TextDonationRequest request) {
        try{return client.post().uri(submitPath).header("Authorization","Bearer "+apiKey).body(request).retrieve().body(DonationReceipt.class);}catch(Exception e){throw new VezilkaIntegrationException("Donation submission failed",e);}
    }

    @Override
    public DonationStatus checkStatus(String vezilkaReference) {
        try{Map body=client.get().uri(statusPath, vezilkaReference).header("Authorization","Bearer "+apiKey).retrieve().body(Map.class);return DonationStatus.valueOf(String.valueOf(body.get("status")).toUpperCase());}catch(Exception e){throw new VezilkaIntegrationException("Donation status check failed",e);}
    }
}

