package mk.ukim.finki.aibotbackend.bot.extraction;

import java.util.List;
import java.util.ArrayList;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import mk.ukim.finki.aibotbackend.bot.browser.PageSnapshot;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.CreateMediaItemDto;
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
            result.add(new CreateExtractedPostDto(externalId, author, text.text(), sourceUrl, postedAt, null, media));
        }
        return result.stream().filter(p -> p.sourceUrl() != null).collect(java.util.stream.Collectors.toMap(
            CreateExtractedPostDto::sourceUrl, p -> p, (a,b) -> a, java.util.LinkedHashMap::new)).values().stream().toList();
    }
}

