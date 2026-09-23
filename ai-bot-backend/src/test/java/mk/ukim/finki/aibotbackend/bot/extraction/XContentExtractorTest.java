package mk.ukim.finki.aibotbackend.bot.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mk.ukim.finki.aibotbackend.bot.browser.PageSnapshot;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.PostEngagement;
import org.junit.jupiter.api.Test;

class XContentExtractorTest {
    private final XContentExtractor extractor = new XContentExtractor();

    @Test
    void extractsPostWithEngagementFromTheActionBar() {
        String html = """
            <article data-testid="tweet">
              <a href="/finki/status/111"><time datetime="2026-07-20T10:15:30.000Z">Jul 20</time></a>
              <div data-testid="tweetText">Денес во Скопје е убаво.</div>
              <div role="group" aria-label="4 replies, 10 reposts, 250 likes, 3 bookmarks, 9876 views"></div>
            </article>
            """;

        List<CreateExtractedPostDto> posts = extractor.extract(snapshot(html));

        assertThat(posts).hasSize(1);
        CreateExtractedPostDto post = posts.getFirst();
        assertThat(post.externalId()).isEqualTo("111");
        assertThat(post.authorHandle()).isEqualTo("finki");
        assertThat(post.engagement()).isEqualTo(new PostEngagement(4L, 10L, 250L, 9_876L));
    }

    @Test
    void fallsBackToIndividualButtons() {
        String html = """
            <article data-testid="tweet">
              <a href="/finki/status/222"><time datetime="2026-07-20T10:15:30.000Z">Jul 20</time></a>
              <div data-testid="tweetText">Објава без групна ознака.</div>
              <button data-testid="reply" aria-label="Reply"></button>
              <button data-testid="retweet" aria-label="2 reposts. Repost"></button>
              <button data-testid="like" aria-label="1,500 Likes. Like"></button>
              <a href="/finki/status/222/analytics" aria-label="20000 views. View post analytics"></a>
            </article>
            """;

        CreateExtractedPostDto post = extractor.extract(snapshot(html)).getFirst();

        assertThat(post.engagement()).isEqualTo(new PostEngagement(0L, 2L, 1_500L, 20_000L));
    }

    @Test
    void marksEngagementUnknownWhenXDoesNotRenderCounters() {
        String html = """
            <article data-testid="tweet">
              <a href="/finki/status/333"><time datetime="2026-07-20T10:15:30.000Z">Jul 20</time></a>
              <div data-testid="tweetText">Без бројачи.</div>
            </article>
            """;

        CreateExtractedPostDto post = extractor.extract(snapshot(html)).getFirst();

        assertThat(post.engagement().isKnown()).isFalse();
    }

    private PageSnapshot snapshot(String html) {
        return new PageSnapshot("https://x.com/search?q=test", "X", html, null);
    }
}
