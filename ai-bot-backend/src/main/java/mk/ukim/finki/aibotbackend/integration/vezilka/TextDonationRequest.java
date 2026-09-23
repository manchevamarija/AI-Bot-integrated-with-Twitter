package mk.ukim.finki.aibotbackend.integration.vezilka;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One item of the Vezilka Public Donation API's {@code POST .../donations/text/}
 * request, matching the field names the API expects (snake_case): {@code source_url},
 * {@code text}, {@code page_title}, {@code retrieved_at}.
 *
 * @param sourceUrl   the page the text was found on; required by the API
 * @param text        the donated Macedonian text
 * @param pageTitle   optional title of the source page
 * @param retrievedAt ISO-8601 timestamp of when the content was read; optional
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextDonationRequest(
    @JsonProperty("source_url") String sourceUrl,
    String text,
    @JsonProperty("page_title") String pageTitle,
    @JsonProperty("retrieved_at") String retrievedAt
) {
}
