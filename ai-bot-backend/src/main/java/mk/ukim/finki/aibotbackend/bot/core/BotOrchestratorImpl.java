package mk.ukim.finki.aibotbackend.bot.core;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import mk.ukim.finki.aibotbackend.bot.llm.BotAction;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.CreateMediaItemDto;
import mk.ukim.finki.aibotbackend.model.enums.BotActionType;
import mk.ukim.finki.aibotbackend.model.enums.MediaType;
import mk.ukim.finki.aibotbackend.model.enums.SessionStatus;
import mk.ukim.finki.aibotbackend.model.exception.InvalidSessionStateException;
import mk.ukim.finki.aibotbackend.model.exception.SessionNotFoundException;
import mk.ukim.finki.aibotbackend.model.exception.SessionStoppedException;
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

        // A PAUSED session can be resumed. Posts saved by the earlier run count
        // toward the limit and are never stored twice.
        Set<String> alreadySaved = new HashSet<>();
        extractedPostService.findAllBySessionId(sessionId)
            .forEach(post -> alreadySaved.add(postKey(post)));
        Map<String, CreateExtractedPostDto> uniquePosts = new LinkedHashMap<>();

        try {
            socialNetworkBot.login();
            for (int targetIndex = 0; targetIndex < session.getTargets().size(); targetIndex++) {
                ensureStillRunning(sessionId);
                ExtractionTarget target = session.getTargets().get(targetIndex);
                int remainingTargets = session.getTargets().size() - targetIndex;
                int remainingCapacity = session.getMaxPosts() - alreadySaved.size() - uniquePosts.size();
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
                    (action, successful) -> {
                        botActionLogService.log(session, action, successful);
                        ensureStillRunning(sessionId);
                    }
                );

                int validCount = 0;
                for (CreateExtractedPostDto post : extracted) {
                    CreateExtractedPostDto filtered = applySessionOptions(post, session);
                    if (filtered == null) {
                        continue;
                    }
                    validCount++;
                    String key = postKey(filtered);
                    if (!alreadySaved.contains(key)) {
                        uniquePosts.putIfAbsent(key, filtered);
                    }
                    if (uniquePosts.size() - beforeTarget >= targetQuota) {
                        break;
                    }
                }

                int newCount = uniquePosts.size() - beforeTarget;
                int duplicateCount = Math.max(0, validCount - newCount);
                logTargetSummary(session, target.getValue(), newCount, duplicateCount);
            }

            savePosts(session, uniquePosts);
            logSessionSummary(session, uniquePosts);
            completeUnlessStopped(sessionId, session, uniquePosts);
        } catch (SessionStoppedException stopped) {
            // The user pressed Stop: keep everything collected from finished
            // targets and leave the session PAUSED so it can be resumed.
            savePosts(session, uniquePosts);
            logStopped(session, uniquePosts);
        } catch (RuntimeException exception) {
            extractionSessionService.fail(sessionId);
            throw exception;
        } finally {
            socialNetworkBot.shutdown();
        }
    }

    private void ensureStillRunning(Long sessionId) {
        SessionStatus status = extractionSessionService.findById(sessionId)
            .map(ExtractionSession::getStatus)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));
        if (status != SessionStatus.RUNNING) {
            throw new SessionStoppedException(sessionId);
        }
    }

    private void completeUnlessStopped(
        Long sessionId,
        ExtractionSession session,
        Map<String, CreateExtractedPostDto> uniquePosts
    ) {
        try {
            extractionSessionService.complete(sessionId);
        } catch (InvalidSessionStateException stoppedAtTheLastMoment) {
            // Stop arrived after the last target finished; the posts are saved.
            logStopped(session, uniquePosts);
        }
    }

    private void savePosts(ExtractionSession session, Map<String, CreateExtractedPostDto> uniquePosts) {
        if (uniquePosts.isEmpty()) {
            return;
        }
        extractedPostService.saveAll(
            uniquePosts.values().stream()
                .map(post -> post.toExtractedPost(session))
                .toList()
        );
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
            media,
            post.engagement()
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
        return postKey(post.sourceUrl(), post.externalId(), post.authorHandle(), post.content());
    }

    private String postKey(ExtractedPost post) {
        return postKey(post.getSourceUrl(), post.getExternalId(), post.getAuthorHandle(), post.getContent());
    }

    private String postKey(String sourceUrl, String externalId, String authorHandle, String content) {
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            return sourceUrl;
        }
        if (externalId != null && !externalId.isBlank()) {
            return externalId;
        }
        return authorHandle + "\u0000" + content;
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

    private void logStopped(ExtractionSession session, Map<String, CreateExtractedPostDto> uniquePosts) {
        String details = "Session stopped by user: %d new post(s) saved".formatted(uniquePosts.size());
        botActionLogService.log(
            session,
            new BotAction(BotActionType.FINISH, null, null, details),
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
        long withEngagement = uniquePosts.values().stream()
            .filter(post -> post.engagement().isKnown())
            .count();

        String details = ("Session completed: %d unique post(s), %d with media, "
            + "%d above Macedonian threshold, %d with engagement counters")
            .formatted(uniquePosts.size(), withMedia, macedonian, withEngagement);
        botActionLogService.log(
            session,
            new BotAction(BotActionType.FINISH, null, null, details),
            true
        );
    }
}
