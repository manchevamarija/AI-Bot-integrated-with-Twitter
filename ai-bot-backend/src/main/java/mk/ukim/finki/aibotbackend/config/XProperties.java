package mk.ukim.finki.aibotbackend.config;

import mk.ukim.finki.aibotbackend.model.enums.ExtractionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "x")
public record XProperties(
    ExtractionMode extractionMode,
    String bearerToken,
    String username,
    String password,
    Boolean browserAutoLogin,
    Integer maxPosts
) {
    public boolean hasBearerToken() {
        return bearerToken != null && !bearerToken.isBlank();
    }

    public ExtractionMode effectiveMode() {
        ExtractionMode configured = extractionMode == null ? ExtractionMode.AUTO : extractionMode;
        return configured == ExtractionMode.AUTO
            ? (hasBearerToken() ? ExtractionMode.LIVE_API : ExtractionMode.LIVE_BROWSER)
            : configured;
    }

    public int effectiveMaxPosts() {
        return maxPosts == null ? 15 : Math.max(1, Math.min(maxPosts, 100));
    }
}
