package mk.ukim.finki.aibotbackend.integration.vezilka;

/**
 * What doniraj.vezilka.ai returns for one submitted text item.
 *
 * <p>Moderation is automatic and synchronous: the Public Donation API decides
 * {@code accepted}/{@code rejected} in the same response, so {@link #accepted()}
 * (which also treats a duplicate as a success — the content is already in the
 * corpus) is known immediately, without polling.</p>
 *
 * @param reference       Vezilka's id for this item, stored as
 *                        {@code DonationBatch.vezilkaReference}
 * @param status          the raw status Vezilka returned, e.g. {@code "accepted"}
 * @param deduped         {@code true} when this exact content was already donated;
 *                        not a failure — the content is in the corpus either way
 * @param awardedPoints   points credited to the donor; {@code null} for API-key
 *                        donations, which never carry points
 * @param rejectionReason why the item was rejected, e.g. {@code "not_macedonian"};
 *                        {@code null} when accepted
 * @param message         a human-readable summary for logs and error messages
 */
public record DonationReceipt(
    String reference,
    String status,
    boolean deduped,
    Integer awardedPoints,
    String rejectionReason,
    String message
) {
    public boolean accepted() {
        return "accepted".equalsIgnoreCase(status) || deduped;
    }

    static DonationReceipt from(VezilkaDonationResult result) {
        String message = result.accepted()
            ? (result.deduped() ? "Already in the corpus (duplicate)" : "Accepted")
            : "Rejected: " + result.rejectionReason();
        return new DonationReceipt(
            result.id(),
            result.status(),
            result.deduped(),
            result.awardedPoints(),
            result.rejectionReason(),
            message
        );
    }
}
