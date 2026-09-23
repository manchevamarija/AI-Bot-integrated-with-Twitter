package mk.ukim.finki.aibotbackend.bot.extraction;

import java.util.List;
import java.util.ArrayList;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import mk.ukim.finki.aibotbackend.bot.browser.PageSnapshot;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.CreateMediaItemDto;
import mk.ukim.finki.aibotbackend.model.dto.PostEngagement;
import mk.ukim.finki.aibotbackend.model.enums.MediaType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Extracts public X post text, metadata and media from browser snapshots.
 */
@Component
public class XContentExtractor implements ContentExtractor {
    @Override
    public List<CreateExtractedPostDto> extract(PageSnapshot snapshot) {
        List<CreateExtractedPostDto> result = new ArrayList<>();
        for (Element tweet : Jsoup.parse(snapshot.domContent(), snapshot.url()).select("article[data-testid=tweet]")) {
            Element text = tweet.selectFirst("[data-testid=tweetText]");
            Element time = tweet.selectFirst("time");
            Element link = time == null ? null : time.closest("a");
            if (text == null || link == null) continue;
            String sourceUrl = link.absUrl("href");
            if (sourceUrl.isBlank()) sourceUrl = "https://x.com" + link.attr("href");
            String externalId = sourceUrl.replaceAll(".*?/status/(\\d+).*", "$1");
            String author = link.attr("href").split("/status/")[0].replace("/", "");
            LocalDateTime postedAt = null;
            try { postedAt = LocalDateTime.ofInstant(Instant.parse(time.attr("datetime")), ZoneOffset.UTC); } catch (Exception ignored) { }
            List<CreateMediaItemDto> media = new ArrayList<>();
            tweet.select("[data-testid=tweetPhoto] img").forEach(img -> media.add(new CreateMediaItemDto(MediaType.IMAGE, img.absUrl("src"), null)));
            tweet.select("video").forEach(video -> { String src = video.absUrl("src"); if (!src.isBlank()) media.add(new CreateMediaItemDto(MediaType.VIDEO, src, null)); });
            result.add(new CreateExtractedPostDto(
                externalId, author, text.text(), sourceUrl, postedAt, null, media, engagement(tweet)));
        }
        return result.stream().filter(p -> p.sourceUrl() != null).collect(java.util.stream.Collectors.toMap(
            CreateExtractedPostDto::sourceUrl, p -> p, (a,b) -> a, java.util.LinkedHashMap::new)).values().stream().toList();
    }

    /**
     * Reads reply, repost, like and view counters. The combined label of the
     * action bar is preferred; individual buttons are the fallback.
     */
    static PostEngagement engagement(Element tweet) {
        Element group = tweet.selectFirst("[role=group][aria-label]");
        if (group != null) {
            PostEngagement fromGroup = EngagementLabelParser.parseGroupLabel(group.attr("aria-label"));
            if (fromGroup.isKnown()) {
                Long views = fromGroup.views() != null ? fromGroup.views() : views(tweet);
                return new PostEngagement(fromGroup.replies(), fromGroup.reposts(), fromGroup.likes(), views);
            }
        }
        PostEngagement fromButtons = new PostEngagement(
            button(tweet, "[data-testid=reply]"),
            button(tweet, "[data-testid=retweet], [data-testid=unretweet]"),
            button(tweet, "[data-testid=like], [data-testid=unlike]"),
            views(tweet)
        );
        return fromButtons.isKnown() ? fromButtons : PostEngagement.UNKNOWN;
    }

    private static Long button(Element tweet, String selector) {
        Element button = tweet.selectFirst(selector);
        if (button == null) {
            return null;
        }
        String label = button.attr("aria-label");
        if (!label.isBlank()) {
            return EngagementLabelParser.parseButtonLabel(label);
        }
        Long visible = EngagementLabelParser.parseCount(button.text());
        return visible == null ? 0L : visible;
    }

    private static Long views(Element tweet) {
        Element analytics = tweet.selectFirst("a[href$=/analytics]");
        return analytics == null ? null : EngagementLabelParser.parseButtonLabel(analytics.attr("aria-label"));
    }
}
