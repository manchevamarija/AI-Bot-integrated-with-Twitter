package mk.ukim.finki.aibotbackend.bot.extraction;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mk.ukim.finki.aibotbackend.model.dto.PostEngagement;

/**
 * Turns the accessibility labels X renders under every post into numbers.
 *
 * <p>X describes the action bar of a post with a label such as
 * {@code "12 replies, 34 reposts, 1,234 likes, 5 bookmarks, 56789 views"},
 * and each button with a label such as {@code "1,234 Likes. Like"}. The
 * browser is started with {@code --lang=en-US}, so the English wording is
 * stable. Visible counters use short forms such as {@code 1.2K}, which are
 * supported as a fallback.</p>
 */
public final class EngagementLabelParser {
    private static final String NUMBER = "(\\d[\\d,.]*\\s*[kmb]?)";
    private static final Pattern REPLIES = metric("repl(?:y|ies)");
    private static final Pattern REPOSTS = metric("(?:reposts?|retweets?)");
    private static final Pattern LIKES = metric("likes?");
    private static final Pattern VIEWS = metric("views?");
    private static final Pattern LEADING_NUMBER =
        Pattern.compile("^\\s*" + NUMBER + "(?![\\d,.])", Pattern.CASE_INSENSITIVE);

    private EngagementLabelParser() {
    }

    /**
     * Parses the combined label of the action bar ({@code role="group"}).
     * A metric that is missing from the label is reported as {@code 0},
     * because X omits zero counters from this label.
     */
    public static PostEngagement parseGroupLabel(String label) {
        if (label == null || label.isBlank()) {
            return PostEngagement.UNKNOWN;
        }
        String normalized = label.toLowerCase(Locale.ROOT);
        return new PostEngagement(
            find(REPLIES, normalized),
            find(REPOSTS, normalized),
            find(LIKES, normalized),
            findOrNull(VIEWS, normalized)
        );
    }

    /**
     * Parses a single button label, e.g. {@code "1,234 Likes. Like"}.
     * A label without a number ({@code "Like"}) means zero.
     */
    public static Long parseButtonLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        Matcher matcher = LEADING_NUMBER.matcher(label);
        return matcher.find() ? parseCount(matcher.group(1)) : 0L;
    }

    /**
     * Converts {@code "1,234"}, {@code "1.2K"}, {@code "3M"} or {@code "987"}
     * into a number. Returns {@code null} for text that is not a count.
     */
    public static Long parseCount(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (value.isEmpty()) {
            return null;
        }
        long multiplier = 1L;
        char suffix = value.charAt(value.length() - 1);
        if (suffix == 'K' || suffix == 'M' || suffix == 'B') {
            multiplier = switch (suffix) {
                case 'K' -> 1_000L;
                case 'M' -> 1_000_000L;
                default -> 1_000_000_000L;
            };
            value = value.substring(0, value.length() - 1);
            // With a suffix the dot or comma is a decimal separator: 1.2K / 1,2K.
            value = value.replace(',', '.');
        } else {
            // Without a suffix separators group thousands: 1,234 / 1.234.
            value = value.replace(",", "").replace(".", "");
        }
        try {
            return new BigDecimal(value)
                .multiply(BigDecimal.valueOf(multiplier))
                .longValue();
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Pattern metric(String word) {
        return Pattern.compile(NUMBER + "\\s+" + word + "\\b");
    }

    private static Long find(Pattern pattern, String label) {
        Long value = findOrNull(pattern, label);
        return value == null ? 0L : value;
    }

    private static Long findOrNull(Pattern pattern, String label) {
        Matcher matcher = pattern.matcher(label);
        return matcher.find() ? parseCount(matcher.group(1)) : null;
    }
}
