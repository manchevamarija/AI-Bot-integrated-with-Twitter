package mk.ukim.finki.aibotbackend.bot.x;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mk.ukim.finki.aibotbackend.config.XProperties;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.CreateMediaItemDto;
import mk.ukim.finki.aibotbackend.model.dto.PostEngagement;
import mk.ukim.finki.aibotbackend.model.enums.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class XApiClient {
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final XProperties properties;

    public XApiClient(RestClient.Builder builder, ObjectMapper objectMapper, XProperties properties) {
        this.client = builder.baseUrl("https://api.x.com").build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<CreateExtractedPostDto> search(ExtractionTarget target) {
        if (!properties.hasBearerToken()) {
            throw new IllegalStateException("X_BEARER_TOKEN is required for LIVE_API mode");
        }
        String response = client.get()
            .uri(uri -> uri.path("/2/tweets/search/recent")
                .queryParam("query", toQuery(target))
                .queryParam("max_results", properties.effectiveMaxPosts())
                .queryParam("tweet.fields", "id,text,author_id,created_at,lang,attachments,public_metrics")
                .queryParam("expansions", "author_id,attachments.media_keys")
                .queryParam("user.fields", "username")
                .queryParam("media.fields", "media_key,type,url,preview_image_url,variants")
                .build())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.bearerToken())
            .retrieve().body(String.class);
        return mapResponse(response);
    }

    String toQuery(ExtractionTarget target) {
        String value = target.getValue().trim();
        return switch (target.getType()) {
            case HASHTAG -> (value.startsWith("#") ? value : "#" + value) + " -is:retweet";
            case PROFILE -> "from:" + value.replace("@", "") + " -is:retweet";
            case KEYWORD -> value + " -is:retweet";
            case FEED_URL -> throw new IllegalArgumentException(
                "LIVE_API supports PROFILE, HASHTAG and KEYWORD targets");
        };
    }

    List<CreateExtractedPostDto> mapResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            Map<String, String> users = new HashMap<>();
            root.path("includes").path("users").forEach(u ->
                users.put(u.path("id").asText(), u.path("username").asText()));
            Map<String, JsonNode> media = new HashMap<>();
            root.path("includes").path("media").forEach(m ->
                media.put(m.path("media_key").asText(), m));
            List<CreateExtractedPostDto> result = new ArrayList<>();
            root.path("data").forEach(tweet -> {
                String id = tweet.path("id").asText();
                String handle = users.getOrDefault(tweet.path("author_id").asText(), "unknown");
                List<CreateMediaItemDto> items = new ArrayList<>();
                tweet.path("attachments").path("media_keys").forEach(key -> {
                    JsonNode m = media.get(key.asText());
                    if (m != null) {
                        boolean video = "video".equals(m.path("type").asText())
                            || "animated_gif".equals(m.path("type").asText());
                        String url = video ? bestMp4Variant(m) : m.path("url").asText();
                        if (!url.isBlank()) items.add(new CreateMediaItemDto(
                            video ? MediaType.VIDEO : MediaType.IMAGE, url, null));
                    }
                });
                String created = tweet.path("created_at").asText();
                LocalDateTime postedAt = created.isBlank()
                    ? null : OffsetDateTime.parse(created).toLocalDateTime();
                result.add(new CreateExtractedPostDto(
                    id, "@" + handle, tweet.path("text").asText(),
                    "https://x.com/" + handle + "/status/" + id,
                    postedAt, 0.0, items, engagement(tweet.path("public_metrics"))));
            });
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse X API response", e);
        }
    }

    private PostEngagement engagement(JsonNode metrics) {
        if (metrics.isMissingNode() || metrics.isNull()) {
            return PostEngagement.UNKNOWN;
        }
        return new PostEngagement(
            count(metrics, "reply_count"),
            count(metrics, "retweet_count"),
            count(metrics, "like_count"),
            count(metrics, "impression_count")
        );
    }

    private Long count(JsonNode metrics, String field) {
        JsonNode value = metrics.path(field);
        return value.isNumber() ? value.asLong() : null;
    }

    private String bestMp4Variant(JsonNode media) {
        String selectedUrl = "";
        int selectedBitRate = -1;
        for (JsonNode variant : media.path("variants")) {
            if (!"video/mp4".equalsIgnoreCase(variant.path("content_type").asText())) {
                continue;
            }
            int bitRate = variant.path("bit_rate").asInt(0);
            if (selectedUrl.isBlank() || bitRate > selectedBitRate) {
                selectedUrl = variant.path("url").asText();
                selectedBitRate = bitRate;
            }
        }
        return selectedUrl;
    }
}
