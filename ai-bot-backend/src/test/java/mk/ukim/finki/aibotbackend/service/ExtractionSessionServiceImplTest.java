package mk.ukim.finki.aibotbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.enums.SessionStatus;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import mk.ukim.finki.aibotbackend.model.exception.InvalidSessionStateException;
import mk.ukim.finki.aibotbackend.repository.ExtractionSessionRepository;
import mk.ukim.finki.aibotbackend.service.domain.impl.ExtractionSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractionSessionServiceImplTest {
    @Mock
    private ExtractionSessionRepository repository;

    private ExtractionSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExtractionSessionServiceImpl(repository);
    }

    @Test
    void startsCreatedSessionAndStampsStartTime() {
        ExtractionSession session = new ExtractionSession(SocialNetwork.X, "test");
        when(repository.findById(1L)).thenReturn(Optional.of(session));
        when(repository.save(session)).thenReturn(session);

        ExtractionSession result = service.start(1L);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.RUNNING);
        assertThat(result.getStartedAt()).isNotNull();
        verify(repository).save(session);
    }

    @Test
    void rejectsStartingCompletedSession() {
        ExtractionSession session = new ExtractionSession(SocialNetwork.X, "test");
        session.setStatus(SessionStatus.COMPLETED);
        when(repository.findById(1L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.start(1L))
            .isInstanceOf(InvalidSessionStateException.class);
    }

    @Test
    void completesOnlyRunningSession() {
        ExtractionSession session = new ExtractionSession(SocialNetwork.X, "test");
        session.setStatus(SessionStatus.RUNNING);
        when(repository.findById(1L)).thenReturn(Optional.of(session));
        when(repository.save(session)).thenReturn(session);

        ExtractionSession result = service.complete(1L);

        assertThat(result.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.getFinishedAt()).isNotNull();
    }
}
