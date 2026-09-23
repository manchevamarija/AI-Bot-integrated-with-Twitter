package mk.ukim.finki.aibotbackend.model.enums;

/**
 * Ranking criteria for the "top posts" view. Each value maps to the
 * persisted {@code ExtractedPost} attribute that is sorted on.
 */
public enum TopPostMetric {
    ENGAGEMENT("engagementScore"),
    LIKES("likeCount"),
    REPOSTS("repostCount"),
    REPLIES("replyCount"),
    VIEWS("viewCount");

    private final String attribute;

    TopPostMetric(String attribute) {
        this.attribute = attribute;
    }

    public String attribute() {
        return attribute;
    }
}
