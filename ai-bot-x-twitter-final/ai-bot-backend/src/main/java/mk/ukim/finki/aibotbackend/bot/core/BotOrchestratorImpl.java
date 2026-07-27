package mk.ukim.finki.aibotbackend.bot.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.dto.CreateMediaItemDto;
import mk.ukim.finki.aibotbackend.model.enums.MediaType;
import mk.ukim.finki.aibotbackend.bot.llm.BotAction;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.enums.BotActionType;
import mk.ukim.finki.aibotbackend.model.enums.SessionStatus;
import mk.ukim.finki.aibotbackend.model.exception.InvalidSessionStateException;
import mk.ukim.finki.aibotbackend.model.exception.SessionNotFoundException;
import mk.ukim.finki.aibotbackend.service.domain.BotActionLogService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractionSessionService;
import org.springframework.stereotype.Service;

@Service
public class BotOrchestratorImpl implements BotOrchestrator {
    private final SocialNetworkBot socialNetworkBot;
    private final ExtractionSessionService extractionSessionService;
    private final ExtractedPostService extractedPostService;
    private final BotActionLogService botActionLogService;

    public BotOrchestratorImpl(
        SocialNetworkBot socialNetworkBot,
        ExtractionSessionService extractionSessionService,
        ExtractedPostService extractedPostService,
        BotActionLogService botActionLogService
    ) {
        this.socialNetworkBot = socialNetworkBot;
        this.extractionSessionService = extractionSessionService;
        this.extractedPostService = extractedPostService;
        this.botActionLogService = botActionLogService;
    }

    @Override
    public synchronized void runSession(Long sessionId) {
        ExtractionSession session = requireRunningSession(sessionId);
        validateSupportedNetwork(session);
        Map<String, CreateExtractedPostDto> uniquePosts = new LinkedHashMap<>();

        try {
            socialNetworkBot.login();
            for (int targetIndex = 0; targetIndex < session.getTargets().size(); targetIndex++) {
                ExtractionTarget target = session.getTargets().get(targetIndex);
                int remainingTargets = session.getTargets().size() - targetIndex;
                int remainingCapacity = session.getMaxPosts() - uniquePosts.size();
                int targetQuota = Math.max(
                    0,
                    (int) Math.ceil((double) remainingCapacity / remainingTargets)
                );
                int beforeTarget = uniquePosts.size();
                if (targetQuota == 0) {
                    logTargetSummary(session, target.getValue(), 0, 0);
                    continue;
                }
                var extracted = socialNetworkBot.execute(
                    target,
                    (action, successful) ->
                        botActionLogService.log(session, action, successful)
                );

                int validCount = 0;
                for (CreateExtractedPostDto post : extracted) {
                    CreateExtractedPostDto filtered = applySessionOptions(post, session);
                    if (filtered == null) {
                        continue;
                    }
                    validCount++;
                    uniquePosts.putIfAbsent(postKey(filtered), filtered);
                    if (uniquePosts.size() - beforeTarget >= targetQuota) {
                        break;
                    }
                }

                int newCount = uniquePosts.size() - beforeTarget;
                int duplicateCount = Math.max(0, validCount - newCount);
                logTargetSummary(session, target.getValue(), newCount, duplicateCount);
            }

            extractedPostService.saveAll(
                uniquePosts.values().stream()
                    .map(post -> post.toExtractedPost(session))
                    .toList()
            );
            logSessionSummary(session, uniquePosts);
            extractionSessionService.complete(sessionId);
        } catch (RuntimeException exception) {
            extractionSessionService.fail(sessionId);
            throw exception;
        } finally {
            socialNetworkBot.shutdown();
        }
    }

    private CreateExtractedPostDto applySessionOptions(
        CreateExtractedPostDto post,
        ExtractionSession session
    ) {
        double confidence = post.macedonianConfidence() == null ? 0.0 : post.macedonianConfidence();
        if (confidence < session.getMinMacedonianConfidence()) {
            return null;
        }

        List<CreateMediaItemDto> media = post.mediaItems() == null
            ? List.of()
            : post.mediaItems().stream()
                .filter(item -> session.getIncludeImages() || item.type() != MediaType.IMAGE)
                .filter(item -> session.getIncludeVideos() || item.type() != MediaType.VIDEO)
                .toList();
        String content = session.getIncludeText() ? post.content() : null;
        if ((content == null || content.isBlank()) && media.isEmpty()) {
            return null;
        }
        return new CreateExtractedPostDto(
            post.externalId(),
            post.authorHandle(),
            content,
            post.sourceUrl(),
            post.postedAt(),
            confidence,
            media
        );
    }

    private ExtractionSession requireRunningSession(Long sessionId) {
        ExtractionSession session = extractionSessionService.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (session.getStatus() != SessionStatus.RUNNING) {
            throw new InvalidSessionStateException(sessionId, session.getStatus());
        }
        return session;
    }

    private void validateSupportedNetwork(ExtractionSession session) {
        if (socialNetworkBot.network() != session.getSocialNetwork()) {
            throw new IllegalArgumentException(
                "The configured bot does not support " + session.getSocialNetwork()
            );
        }
    }

    private String postKey(CreateExtractedPostDto post) {
        if (post.sourceUrl() != null && !post.sourceUrl().isBlank()) {
            return post.sourceUrl();
        }
        if (post.externalId() != null && !post.externalId().isBlank()) {
            return post.externalId();
        }
        return post.authorHandle() + "\u0000" + post.content();
    }

    private void logTargetSummary(
        ExtractionSession session,
        String target,
        int newCount,
        int duplicateCount
    ) {
        String details = "Target '%s': %d new post(s), %d duplicate(s) ignored"
            .formatted(target, newCount, duplicateCount);
        botActionLogService.log(
            session,
            new BotAction(BotActionType.EXTRACT, target, null, details),
            true
        );
    }

    private void logSessionSummary(
        ExtractionSession session,
        Map<String, CreateExtractedPostDto> uniquePosts
    ) {
        long withMedia = uniquePosts.values().stream()
            .filter(post -> post.mediaItems() != null && !post.mediaItems().isEmpty())
            .count();
        long macedonian = uniquePosts.values().stream()
            .filter(post -> post.macedonianConfidence() != null)
            .filter(post -> post.macedonianConfidence() >= 0.5)
            .count();

        String details = "Session completed: %d unique post(s), %d with media, %d above Macedonian threshold"
            .formatted(uniquePosts.size(), withMedia, macedonian);
        botActionLogService.log(
            session,
            new BotAction(BotActionType.FINISH, null, null, details),
            true
        );
    }
}
