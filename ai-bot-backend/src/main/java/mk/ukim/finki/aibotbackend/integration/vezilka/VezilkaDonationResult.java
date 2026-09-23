package mk.ukim.finki.aibotbackend.integration.vezilka;

/**
 * One entry of the Vezilka API's {@code results} array — the per-item outcome of
 * a donation submission. Field names already match the API's JSON (camelCase),
 * so no {@code @JsonProperty} mapping is needed.
 *
 * @param status one of {@code accepted}, {@code rejected} (or similar); see the
 *               {@link #accepted()} helper
 */
record VezilkaDonationResult(
    String id,
    String status,
    String contentType,
    boolean deduped,
    Integer awardedPoints,
    String rejectionReason
) {
    boolean accepted() {
        return "accepted".equalsIgnoreCase(status);
    }
}
