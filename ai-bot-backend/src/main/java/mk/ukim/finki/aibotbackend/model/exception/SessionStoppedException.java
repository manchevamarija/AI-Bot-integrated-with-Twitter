package mk.ukim.finki.aibotbackend.model.exception;

/**
 * Signals that a user stopped a running extraction session. The orchestrator
 * catches it, keeps what was already collected and ends the run cleanly.
 */
public class SessionStoppedException extends RuntimeException {
    public SessionStoppedException(Long sessionId) {
        super("Extraction session " + sessionId + " was stopped by the user");
    }
}
