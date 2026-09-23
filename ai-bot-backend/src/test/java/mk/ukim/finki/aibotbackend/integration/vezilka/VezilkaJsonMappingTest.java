package mk.ukim.finki.aibotbackend.integration.vezilka;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Verifies the request/response shapes match the Vezilka Public Donation API
 * documentation exactly: snake_case request fields wrapped in an
 * {@code {"items": [...]}} envelope, and camelCase response fields.
 */
class VezilkaJsonMappingTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void requestUsesSnakeCaseFieldsInAnItemsEnvelope() throws Exception {
        TextDonationRequest item = new TextDonationRequest(
            "https://x.com/finki/status/1",
            "Македонски текст.",
            "@finki",
            "2026-07-30T10:00:00Z"
        );

        String json = mapper.writeValueAsString(VezilkaDonationEnvelope.ofOne(item));

        assertThat(json)
            .contains("\"items\"")
            .contains("\"source_url\":\"https://x.com/finki/status/1\"")
            .contains("\"text\":\"Македонски текст.\"")
            .contains("\"page_title\":\"@finki\"")
            .contains("\"retrieved_at\":\"2026-07-30T10:00:00Z\"")
            .doesNotContain("sourceUrl")
            .doesNotContain("pageTitle")
            .doesNotContain("retrievedAt");
    }

    @Test
    void omitsNullOptionalFieldsFromTheRequest() throws Exception {
        TextDonationRequest item = new TextDonationRequest("https://x.com/a", "Текст", null, null);

        String json = mapper.writeValueAsString(item);

        assertThat(json).doesNotContain("page_title").doesNotContain("retrieved_at");
    }

    @Test
    void parsesTheDocumentedResponseShape() throws Exception {
        String body = """
            {
              "results": [
                {
                  "id": "9f2c1e8a-...",
                  "status": "accepted",
                  "contentType": "text",
                  "deduped": false,
                  "awardedPoints": 20,
                  "rejectionReason": null
                }
              ],
              "total": 1,
              "accepted": 1,
              "rejected": 0,
              "duplicates": 0
            }
            """;

        VezilkaDonationResponse response = mapper.readValue(body, VezilkaDonationResponse.class);

        assertThat(response.results()).hasSize(1);
        VezilkaDonationResult result = response.results().getFirst();
        assertThat(result.id()).isEqualTo("9f2c1e8a-...");
        assertThat(result.accepted()).isTrue();
        assertThat(result.deduped()).isFalse();
        assertThat(result.awardedPoints()).isEqualTo(20);

        DonationReceipt receipt = DonationReceipt.from(result);
        assertThat(receipt.accepted()).isTrue();
        assertThat(receipt.reference()).isEqualTo("9f2c1e8a-...");
    }

    @Test
    void aDuplicateCountsAsAcceptedEvenWithoutTheAcceptedStatus() {
        VezilkaDonationResult duplicate = new VezilkaDonationResult(
            "id-1", "accepted", "text", true, null, null);

        assertThat(DonationReceipt.from(duplicate).accepted()).isTrue();
    }

    @Test
    void aRejectedItemCarriesItsReason() {
        VezilkaDonationResult rejected = new VezilkaDonationResult(
            "id-2", "rejected", "text", false, null, "not_macedonian");

        DonationReceipt receipt = DonationReceipt.from(rejected);

        assertThat(receipt.accepted()).isFalse();
        assertThat(receipt.rejectionReason()).isEqualTo("not_macedonian");
    }
}
