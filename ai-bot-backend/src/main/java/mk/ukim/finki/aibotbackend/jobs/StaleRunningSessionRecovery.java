package mk.ukim.finki.aibotbackend.jobs;

import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.aibotbackend.model.enums.SessionStatus;
import mk.ukim.finki.aibotbackend.service.domain.ExtractionSessionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * A browser extraction cannot continue after the backend process has stopped.
 * Mark sessions left in RUNNING state as failed after a restart so the UI does
 * not present them as active forever.
 */
@Component
@Slf4j
public class StaleRunningSessionRecovery {
    private final ExtractionSessionService extractionSessionService;

    public StaleRunningSessionRecovery(ExtractionSessionService extractionSessionService) {
        this.extractionSessionService = extractionSessionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedSessions() {
        var staleSessionIds = extractionSessionService.findAll().stream()
                .filter(session -> session.getStatus() == SessionStatus.RUNNING)
                .map(session -> session.getId())
                .toList();

        staleSessionIds.forEach(id -> {
            try {
                extractionSessionService.fail(id);
            } catch (RuntimeException exception) {
                log.warn("Could not recover interrupted extraction session {}", id, exception);
            }
        });

        if (!staleSessionIds.isEmpty()) {
            log.info("Marked {} interrupted extraction session(s) as FAILED",
                    staleSessionIds.size());
        }
    }
}
