package mk.ukim.finki.aibotbackend.integration.vezilka;

import java.util.List;

/**
 * The {@code {"items": [...]}} envelope the Vezilka Public Donation API expects.
 * The documentation recommends this shape even for a single item, since the same
 * shape works for one or many.
 */
record VezilkaDonationEnvelope(List<TextDonationRequest> items) {
    static VezilkaDonationEnvelope ofOne(TextDonationRequest item) {
        return new VezilkaDonationEnvelope(List.of(item));
    }
}
