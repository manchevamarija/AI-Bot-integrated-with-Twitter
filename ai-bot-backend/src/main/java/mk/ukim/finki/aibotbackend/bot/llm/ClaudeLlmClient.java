package mk.ukim.finki.aibotbackend.bot.llm;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mk.ukim.finki.aibotbackend.bot.browser.PageSnapshot;
import mk.ukim.finki.aibotbackend.model.enums.BotActionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Claude-backed decision client with a deterministic read-only fallback.
 */
@Component
public class ClaudeLlmClient implements LlmClient {
    private static final Pattern GOAL_URL = Pattern.compile("https://x\\.com/[^,\\s]+");
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public ClaudeLlmClient(ObjectMapper mapper, @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
                         @Value("${anthropic.api-key:}") String apiKey,
                         @Value("${anthropic.model:claude-sonnet-4-6}") String model,
                         @Value("${anthropic.enabled:false}") boolean enabled) {
        this.mapper = mapper; this.apiKey = apiKey; this.model = model; this.enabled = enabled;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }
    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (apiKey.isBlank()) throw new IllegalStateException("ANTHROPIC_API_KEY is not configured");
        Map<String,Object> body = Map.of(
            "model", model,
            "max_tokens", 1024,
            "temperature", 0.1,
            "system", systemPrompt,
            "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );
        JsonNode response = client.post().uri("/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .body(body).retrieve().body(JsonNode.class);
        return response.path("content").path(0).path("text").asText();
    }

    @Override
    public BotDecision decideNextAction(PageSnapshot snapshot, String goal, List<BotAction> history) {
        if (!enabled || apiKey.isBlank()) {
            return fallbackDecision(
                snapshot,
                goal,
                history,
                "Safe read-only browser workflow",
                "Claude integration is disabled"
            );
        }

        String system = "You control a browser on X to collect public Macedonian posts. Return JSON only: "
            + "{\"action\":{\"type\":\"NAVIGATE|CLICK|TYPE|SCROLL|WAIT|EXTRACT|LOGIN|FINISH\",\"target\":null,\"value\":null,\"reasoning\":\"...\"},\"goalReached\":false,\"rationale\":\"...\"}. "
            + "Use CSS selectors when possible. Never open private content, post, like, follow, message, or modify an account.";
        String dom = snapshot.domContent() == null ? "" : snapshot.domContent();
        if (dom.length() > 80_000) dom = dom.substring(0, 80_000);
        String user = "Goal: " + goal + "\nURL: " + snapshot.url() + "\nHistory: " + history + "\nDOM:\n" + dom;
        try {
            String raw = extractJsonObject(complete(system, user));
            JsonNode root = mapper.readTree(raw); JsonNode a = root.path("action");
            BotAction action = new BotAction(BotActionType.valueOf(a.path("type").asText()),
                nullIfBlank(a.path("target").asText(null)), nullIfBlank(a.path("value").asText(null)), a.path("reasoning").asText());
            return new BotDecision(action, root.path("goalReached").asBoolean(false), root.path("rationale").asText());
        } catch (Exception e) {
            return fallbackDecision(
                snapshot,
                goal,
                history,
                "Claude API unavailable; using safe read-only browser fallback",
                e.getMessage()
            );
        }
    }

    /**
     * Keeps read-only live extraction operational if Claude is temporarily
     * unavailable (for example, exhausted API credits). Claude remains the
     * primary decision maker whenever the API succeeds.
     */
    private BotDecision fallbackDecision(
        PageSnapshot snapshot,
        String goal,
        List<BotAction> history,
        String reason,
        String detail
    ) {
        if (history.isEmpty()) {
            Matcher matcher = GOAL_URL.matcher(goal);
            if (!matcher.find()) {
                return new BotDecision(
                    new BotAction(BotActionType.FINISH, null, null,
                        reason + ": target URL was not found"),
                    false,
                    detail
                );
            }
            return new BotDecision(
                new BotAction(BotActionType.NAVIGATE, matcher.group(), null, reason),
                false,
                detail
            );
        }

        long extracts = history.stream().filter(a -> a.type() == BotActionType.EXTRACT).count();
        long scrolls = history.stream().filter(a -> a.type() == BotActionType.SCROLL).count();
        BotActionType last = history.get(history.size() - 1).type();

        if (extracts >= 4) {
            return new BotDecision(
                new BotAction(BotActionType.FINISH, null, null,
                    reason + ": extraction passes completed"),
                false,
                detail
            );
        }
        if (last == BotActionType.NAVIGATE || last == BotActionType.SCROLL) {
            return new BotDecision(
                new BotAction(BotActionType.WAIT, null, null,
                    reason + ": waiting for X results to render"),
                false,
                detail
            );
        }
        if (last == BotActionType.EXTRACT && scrolls < 3) {
            return new BotDecision(
                new BotAction(BotActionType.SCROLL, null, null,
                    reason + ": loading more public results"),
                false,
                detail
            );
        }
        return new BotDecision(
            new BotAction(BotActionType.EXTRACT, "article[data-testid='tweet']", null,
                reason + ": extracting visible real X posts"),
            false,
            detail
        );
    }

    private String extractJsonObject(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("Claude returned an empty response");
        }
        for (int start = response.indexOf('{'); start >= 0; start = response.indexOf('{', start + 1)) {
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            for (int i = start; i < response.length(); i++) {
                char c = response.charAt(i);
                if (inString) {
                    if (escaped) escaped = false;
                    else if (c == '\\') escaped = true;
                    else if (c == '"') inString = false;
                } else if (c == '"') {
                    inString = true;
                } else if (c == '{') {
                    depth++;
                } else if (c == '}' && --depth == 0) {
                    String candidate = response.substring(start, i + 1);
                    try {
                        mapper.readTree(candidate);
                        return candidate;
                    } catch (Exception ignored) {
                        break;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Claude response did not contain valid JSON");
    }

    private String nullIfBlank(String value) { return value == null || value.isBlank() || "null".equals(value) ? null : value; }
}

