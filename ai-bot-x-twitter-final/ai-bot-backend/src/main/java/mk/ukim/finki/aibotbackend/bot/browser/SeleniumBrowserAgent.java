package mk.ukim.finki.aibotbackend.bot.browser;

import java.util.Base64;
import mk.ukim.finki.aibotbackend.config.BotProperties;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;
import java.time.Duration;

/**
 * Selenium-backed browser implementation for read-only X navigation.
 */
@Component
public class SeleniumBrowserAgent implements BrowserAgent {
    private final BotProperties properties;
    private WebDriver driver;

    public SeleniumBrowserAgent(BotProperties properties) { this.properties = properties; }

    @Override
    public void start() {
        if (driver != null) return;
        ChromeOptions options = new ChromeOptions();
        if (Boolean.TRUE.equals(properties.headless())) options.addArguments("--headless=new");
        options.addArguments(
                "--disable-notifications",
                "--disable-background-mode",
                "--no-first-run",
                "--no-default-browser-check",
                "--remote-debugging-port=0",
                "--window-size=1440,1000",
                "--lang=en-US"
        );
        if (properties.profileDirectory() != null && !properties.profileDirectory().isBlank()) {
            options.addArguments("--user-data-dir=" + properties.profileDirectory());
        }
        driver = new ChromeDriver(options);
    }

    @Override
    public void navigateTo(String url) {
        ensureStarted();
        // X has no supported Macedonian `lang:mk` search operator. Claude may
        // still suggest it, so remove it before navigating and let the local
        // Macedonian detector classify the real search results afterwards.
        String safeUrl = url == null ? null : url
                .replaceAll("(?i)(\\+|%20|\\s)lang(?:%3A|:)mk", "");
        driver.get(safeUrl);
    }

    @Override
    public void click(String elementDescription) {
        WebElement element = resolve(elementDescription);
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(element)).click();
    }

    @Override
    public void type(String elementDescription, String text) {
        WebElement element = resolve(elementDescription); element.clear(); element.sendKeys(text);
    }

    @Override
    public void scrollDown() {
        ensureStarted(); ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, Math.max(window.innerHeight * .85, 700))");
    }

    @Override
    public byte[] takeScreenshot() {
        ensureStarted(); return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Override
    public PageSnapshot snapshot() {
        ensureStarted();
        String html = driver.getPageSource();
        if (html.length() > 350_000) html = html.substring(0, 350_000);
        return new PageSnapshot(driver.getCurrentUrl(), driver.getTitle(), html,
            Base64.getEncoder().encodeToString(takeScreenshot()));
    }

    @Override
    public void close() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (RuntimeException ignored) {
                // The browser may already be closed after a failed navigation.
            } finally {
                driver = null;
            }
        }
    }

    private void ensureStarted() { if (driver == null) start(); }
    private WebElement resolve(String description) {
        ensureStarted();
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Element target is required");
        boolean looksLikeSelector = description.matches(".*[\\[\\].#>:].*");
        if (looksLikeSelector) {
            By selector = By.cssSelector(description);
            return new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver ->
                webDriver.findElements(selector).stream()
                    .filter(WebElement::isDisplayed)
                    .filter(WebElement::isEnabled)
                    .findFirst()
                    .orElse(null)
            );
        }
        String needle = description.toLowerCase();
        return new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver -> {
            var candidates = webDriver.findElements(By.cssSelector("input,button,a,[role='button'],textarea"))
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .toList();
            return candidates.stream()
                .filter(e -> safe(e.getText()).trim().equalsIgnoreCase(description.trim()))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                    .filter(e -> searchableText(e).contains(needle))
                    .findFirst()
                    .orElse(null));
        });
    }

    private String searchableText(WebElement element) {
        String parentText = "";
        try {
            parentText = element.findElement(By.xpath("..")).getText();
        } catch (WebDriverException ignored) {
            // The element may be re-rendered while X transitions between login steps.
        }
        return String.join(" ",
            safe(element.getText()),
            safe(element.getAttribute("aria-label")),
            safe(element.getAttribute("placeholder")),
            safe(element.getAttribute("name")),
            safe(element.getAttribute("type")),
            safe(parentText)
        ).toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

