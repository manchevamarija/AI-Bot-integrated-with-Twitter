package mk.ukim.finki.aibotbackend.bot.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import mk.ukim.finki.aibotbackend.bot.browser.PageSnapshot;
import mk.ukim.finki.aibotbackend.model.enums.BotActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClaudeLlmClientTest {
    private ClaudeLlmClient client;
    private final PageSnapshot snapshot =
        new PageSnapshot("about:blank", "X", "<html></html>", null);
    private final String goal =
        "Navigate to https://x.com/search?q=%D0%A1%D0%BA%D0%BE%D0%BF%D1%98%D0%B5, extract posts";

    @BeforeEach
    void setUp() {
        client = new ClaudeLlmClient(
            new ObjectMapper(),
            "https://api.anthropic.com",
            "",
            "claude-sonnet-4-6",
            false
        );
    }

    @Test
    void disabledClaudeStartsWithSafeNavigation() {
        BotDecision decision = client.decideNextAction(snapshot, goal, List.of());

        assertThat(decision.action().type()).isEqualTo(BotActionType.NAVIGATE);
        assertThat(decision.action().target()).startsWith("https://x.com/search");
        assertThat(decision.action().reasoning()).contains("Safe read-only browser workflow");
    }

    @Test
    void deterministicWorkflowFinishesAfterFourExtractionPasses() {
        List<BotAction> history = new ArrayList<>();
        for (int index = 0; index < 4; index++) {
            history.add(new BotAction(BotActionType.EXTRACT, "article", null, "extract"));
        }

        BotDecision decision = client.decideNextAction(snapshot, goal, history);

        assertThat(decision.action().type()).isEqualTo(BotActionType.FINISH);
    }

    @Test
    void missingTargetUrlFinishesSafely() {
        BotDecision decision = client.decideNextAction(
            snapshot,
            "Extract Macedonian posts",
            List.of()
        );

        assertThat(decision.action().type()).isEqualTo(BotActionType.FINISH);
        assertThat(decision.goalReached()).isFalse();
    }
}
