package mk.ukim.finki.aibotbackend.bot.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import mk.ukim.finki.aibotbackend.bot.llm.BotAction;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.PostEngagement;
import mk.ukim.finki.aibotbackend.model.enums.BotActionType;
import mk.ukim.finki.aibotbackend.model.enums.SessionStatus;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import mk.ukim.finki.aibotbackend.model.enums.TargetType;
import mk.ukim.finki.aibotbackend.service.domain.BotActionLogService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractionSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BotOrchestratorImplTest {
    private static final Long SESSION_ID = 1L;

    private SocialNetworkBot bot;
    private ExtractionSessionService sessionService;
    private ExtractedPostService postService;
    private BotActionLogService logService;
    private BotOrchestratorImpl orchestrator;
    private ExtractionSession session;

    @BeforeEach
    void setUp() {
        bot = mock(SocialNetworkBot.class);
        sessionService = mock(ExtractionSessionService.class);
        postService = mock(ExtractedPostService.class);
        logService = mock(BotActionLogService.class);
        orchestrator = new BotOrchestratorImpl(bot, sessionService, postService, logService);

        session = new ExtractionSession(SocialNetwork.X, "test");
        session.setStatus(SessionStatus.RUNNING);
        session.getTargets().add(new ExtractionTarget(TargetType.KEYWORD, "Скопје", session));
        when(bot.network()).thenReturn(SocialNetwork.X);
        when(sessionService.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(postService.findAllBySessionId(SESSION_ID)).thenReturn(List.of());
    }

    @Test
    void stopDuringExtractionKeepsSessionPausedInsteadOfFailingIt() {
        when(bot.execute(any(), any())).thenAnswer(invocation -> {
            BotStepListener listener = invocation.getArgument(1);
            session.setStatus(SessionStatus.PAUSED);
            listener.onStep(new BotAction(BotActionType.NAVIGATE, "https://x.com", null, "test"), true);
            return List.of(post("https://x.com/a/status/1", null));
        });

        orchestrator.runSession(SESSION_ID);

        verify(sessionService, never()).complete(SESSION_ID);
        verify(sessionService, never()).fail(SESSION_ID);
        verify(bot).shutdown();
        ArgumentCaptor<BotAction> actions = ArgumentCaptor.forClass(BotAction.class);
        verify(logService, org.mockito.Mockito.atLeastOnce()).log(eq(session), actions.capture(), anyBoolean());
        assertThat(actions.getAllValues())
            .anyMatch(action -> action.reasoning() != null && action.reasoning().contains("stopped by user"));
    }

    @Test
    void resumedSessionDoesNotSaveAlreadyStoredPostsAgain() {
        ExtractedPost existing = new ExtractedPost(
            session, "1", "a", "стара објава", "https://x.com/a/status/1", null, 0.9);
        when(postService.findAllBySessionId(SESSION_ID)).thenReturn(List.of(existing));
        when(bot.execute(any(), any())).thenReturn(List.of(
            post("https://x.com/a/status/1", null),
            post("https://x.com/b/status/2", null)
        ));

        orchestrator.runSession(SESSION_ID);

        List<ExtractedPost> saved = capturedSavedPosts();
        assertThat(saved).extracting(ExtractedPost::getSourceUrl)
            .containsExactly("https://x.com/b/status/2");
        verify(sessionService).complete(SESSION_ID);
    }

    @Test
    void engagementCountersAreStoredWithThePost() {
        when(bot.execute(any(), any())).thenReturn(List.of(
            post("https://x.com/a/status/1", new PostEngagement(3L, 5L, 100L, 4000L))
        ));

        orchestrator.runSession(SESSION_ID);

        ExtractedPost saved = capturedSavedPosts().getFirst();
        assertThat(saved.getLikeCount()).isEqualTo(100L);
        assertThat(saved.getRepostCount()).isEqualTo(5L);
        assertThat(saved.getReplyCount()).isEqualTo(3L);
        assertThat(saved.getViewCount()).isEqualTo(4000L);
        assertThat(saved.getEngagementScore()).isEqualTo(100L + 2 * 5L + 2 * 3L);
    }

    @SuppressWarnings("unchecked")
    private List<ExtractedPost> capturedSavedPosts() {
        ArgumentCaptor<List<ExtractedPost>> captor = ArgumentCaptor.forClass(List.class);
        verify(postService).saveAll(captor.capture());
        return captor.getValue();
    }

    private CreateExtractedPostDto post(String url, PostEngagement engagement) {
        return new CreateExtractedPostDto(
            url.replaceAll(".*/status/", ""), "author", "Ова е македонска објава за Скопје.",
            url, null, 0.9, List.of(), engagement);
    }
}
