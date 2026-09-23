package mk.ukim.finki.aibotbackend.bot.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import mk.ukim.finki.aibotbackend.model.dto.PostEngagement;
import org.junit.jupiter.api.Test;

class EngagementLabelParserTest {
    @Test
    void parsesPlainAndGroupedNumbers() {
        assertThat(EngagementLabelParser.parseCount("987")).isEqualTo(987L);
        assertThat(EngagementLabelParser.parseCount("1,234")).isEqualTo(1_234L);
        assertThat(EngagementLabelParser.parseCount("12,345,678")).isEqualTo(12_345_678L);
    }

    @Test
    void parsesShortFormCounters() {
        assertThat(EngagementLabelParser.parseCount("1.2K")).isEqualTo(1_200L);
        assertThat(EngagementLabelParser.parseCount("15K")).isEqualTo(15_000L);
        assertThat(EngagementLabelParser.parseCount("3.4M")).isEqualTo(3_400_000L);
    }

    @Test
    void rejectsTextThatIsNotACount() {
        assertThat(EngagementLabelParser.parseCount("Like")).isNull();
        assertThat(EngagementLabelParser.parseCount("")).isNull();
        assertThat(EngagementLabelParser.parseCount(null)).isNull();
    }

    @Test
    void readsTheCombinedActionBarLabel() {
        PostEngagement engagement = EngagementLabelParser.parseGroupLabel(
            "12 replies, 34 reposts, 1,234 likes, 5 bookmarks, 56789 views");

        assertThat(engagement).isEqualTo(new PostEngagement(12L, 34L, 1_234L, 56_789L));
        assertThat(engagement.score()).isEqualTo(1_234L + 2 * 34L + 2 * 12L);
    }

    @Test
    void missingCountersInTheActionBarMeanZeroExceptViews() {
        PostEngagement engagement = EngagementLabelParser.parseGroupLabel("1 reply, 7 likes");

        assertThat(engagement).isEqualTo(new PostEngagement(1L, 0L, 7L, null));
    }

    @Test
    void readsSingleButtonLabels() {
        assertThat(EngagementLabelParser.parseButtonLabel("1,234 Likes. Like")).isEqualTo(1_234L);
        assertThat(EngagementLabelParser.parseButtonLabel("3 Replies. Reply")).isEqualTo(3L);
        assertThat(EngagementLabelParser.parseButtonLabel("Like")).isZero();
        assertThat(EngagementLabelParser.parseButtonLabel("5678 views. View post analytics")).isEqualTo(5_678L);
    }

    @Test
    void unknownEngagementHasNoScore() {
        assertThat(PostEngagement.UNKNOWN.isKnown()).isFalse();
        assertThat(PostEngagement.UNKNOWN.score()).isNull();
    }
}
