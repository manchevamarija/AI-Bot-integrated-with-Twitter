package mk.ukim.finki.aibotbackend.integration.vezilka;

import java.util.List;

/**
 * The Vezilka API's response to a donation submission: one {@link VezilkaDonationResult}
 * per submitted item, in the same order, plus summary counts.
 */
record VezilkaDonationResponse(
    List<VezilkaDonationResult> results,
    int total,
    int accepted,
    int rejected,
    int duplicates
) {
}
