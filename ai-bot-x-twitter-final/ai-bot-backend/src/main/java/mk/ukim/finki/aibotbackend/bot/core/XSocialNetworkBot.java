package mk.ukim.finki.aibotbackend.bot.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import mk.ukim.finki.aibotbackend.bot.browser.BrowserAgent;
import mk.ukim.finki.aibotbackend.bot.extraction.ContentExtractor;
import mk.ukim.finki.aibotbackend.bot.extraction.LanguageDetector;
import mk.ukim.finki.aibotbackend.bot.llm.LlmClient;
import mk.ukim.finki.aibotbackend.config.BotProperties;
import mk.ukim.finki.aibotbackend.config.XProperties;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.enums.ExtractionMode;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import org.springframework.stereotype.Component;

@Component
public class XSocialNetworkBot extends AbstractSocialNetworkBot {
    private final XProperties properties;

    public XSocialNetworkBot(
        BrowserAgent browserAgent,
        LlmClient llmClient,
        ContentExtractor contentExtractor,
        LanguageDetector languageDetector,
        BotProperties botProperties,
        XProperties properties
    ) {
        super(browserAgent, llmClient, contentExtractor, languageDetector, botProperties);
        this.properties = properties;
    }

    @Override
    public SocialNetwork network() {
        return SocialNetwork.X;
    }

    @Override
    public void login() {
        if (properties.effectiveMode() != ExtractionMode.LIVE_BROWSER) return;
        browserAgent.start();
        if (!Boolean.TRUE.equals(properties.browserAutoLogin())
            || properties.username() == null || properties.username().isBlank()
            || properties.password() == null || properties.password().isBlank()) return;
        browserAgent.navigateTo("https://x.com/i/flow/login");
        browserAgent.type("Email or username", properties.username());
        browserAgent.click("Continue");
        browserAgent.type("input[type='password']", properties.password());
        browserAgent.click("Log in");
    }

    @Override
    protected String buildGoal(ExtractionTarget target) {
        String value = target.getValue().trim();
        String url = switch (target.getType()) {
            case PROFILE -> "https://x.com/" + value.replace("@", "");
            case HASHTAG -> "https://x.com/search?q=" + enc(
                value.startsWith("#") ? value : "#" + value) + "&src=typed_query&f=live";
            // X does not support Macedonian as a `lang:` search operator.
            // Fetch the real keyword results and apply our Macedonian detector afterwards.
            case KEYWORD -> "https://x.com/search?q=" + enc(value)
                + "&src=typed_query&f=live";
            case FEED_URL -> value;
        };
        return "Navigate to " + url
            + ", inspect only public results, extract visible posts and their image/video metadata, "
            + "scroll up to three times if needed, then finish. Never post, like, follow or change account state.";
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
