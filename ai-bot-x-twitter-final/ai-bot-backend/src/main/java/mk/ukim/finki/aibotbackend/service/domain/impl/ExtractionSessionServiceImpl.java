package mk.ukim.finki.aibotbackend.service.domain.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.enums.SessionStatus;
import mk.ukim.finki.aibotbackend.model.exception.InvalidSessionStateException;
import mk.ukim.finki.aibotbackend.model.exception.SessionNotFoundException;
import mk.ukim.finki.aibotbackend.repository.ExtractionSessionRepository;
import mk.ukim.finki.aibotbackend.service.domain.ExtractionSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionSessionServiceImpl implements ExtractionSessionService {
    private final ExtractionSessionRepository extractionSessionRepository;

    public ExtractionSessionServiceImpl(ExtractionSessionRepository extractionSessionRepository) {
        this.extractionSessionRepository = extractionSessionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtractionSession> findAll() {
        return extractionSessionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExtractionSession> findById(Long id) {
        return extractionSessionRepository.findById(id);
    }

    @Override
    @Transactional
    public ExtractionSession create(ExtractionSession session) {
        session.setStatus(SessionStatus.CREATED);
        session.setStartedAt(null);
        session.setFinishedAt(null);
        return extractionSessionRepository.save(session);
    }

    @Override
    @Transactional
    public ExtractionSession start(Long id) {
        ExtractionSession session = requireSession(id);
        requireStatus(session, SessionStatus.CREATED, SessionStatus.PAUSED);

        session.setStatus(SessionStatus.RUNNING);
        session.setFinishedAt(null);
        if (session.getStartedAt() == null) {
            session.setStartedAt(LocalDateTime.now());
        }
        return extractionSessionRepository.save(session);
    }

    @Override
    @Transactional
    public ExtractionSession stop(Long id) {
        ExtractionSession session = requireSession(id);
        requireStatus(session, SessionStatus.RUNNING);

        session.setStatus(SessionStatus.PAUSED);
        return extractionSessionRepository.save(session);
    }

    @Override
    @Transactional
    public ExtractionSession complete(Long id) {
        ExtractionSession session = requireSession(id);
        requireStatus(session, SessionStatus.RUNNING);

        session.setStatus(SessionStatus.COMPLETED);
        session.setFinishedAt(LocalDateTime.now());
        return extractionSessionRepository.save(session);
    }

    @Override
    @Transactional
    public ExtractionSession fail(Long id) {
        ExtractionSession session = requireSession(id);
        session.setStatus(SessionStatus.FAILED);
        session.setFinishedAt(LocalDateTime.now());
        return extractionSessionRepository.save(session);
    }

    private ExtractionSession requireSession(Long id) {
        return extractionSessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));
    }

    private void requireStatus(ExtractionSession session, SessionStatus... allowedStatuses) {
        for (SessionStatus allowedStatus : allowedStatuses) {
            if (session.getStatus() == allowedStatus) {
                return;
            }
        }
        throw new InvalidSessionStateException(session.getId(), session.getStatus());
    }
}
