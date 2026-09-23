package mk.ukim.finki.aibotbackend.model.dto;

/**
 * Public engagement counters of a post as shown in the X interface.
 *
 * <p>Each counter is {@code null} when X did not render it, so an unknown
 * value is never confused with a real zero.</p>
 *
 * <p>The engagement score weights interactions that require more effort:
 * a like counts once, a repost or a reply counts twice. Views are shown but
 * are not part of the score because X counts passive impressions.</p>
 */
public record PostEngagement(
    Long replies,
    Long reposts,
    Long likes,
    Long views
) {
    public static final PostEngagement UNKNOWN = new PostEngagement(null, null, null, null);

    public boolean isKnown() {
        return replies != null || reposts != null || likes != null || views != null;
    }

    public Long score() {
        if (replies == null && reposts == null && likes == null) {
            return null;
        }
        return orZero(likes) + 2 * orZero(reposts) + 2 * orZero(replies);
    }

    private static long orZero(Long value) {
        return value == null ? 0L : value;
    }
}
