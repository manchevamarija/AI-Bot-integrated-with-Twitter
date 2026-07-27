package mk.ukim.finki.aibotbackend.repository;

import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExtractionSessionRepository extends JpaRepository<ExtractionSession, Long> {
    @Override
    @EntityGraph(attributePaths = "targets")
    Optional<ExtractionSession> findById(Long id);

    //  (e.g. find sessions by status, by social network, ...).
}

