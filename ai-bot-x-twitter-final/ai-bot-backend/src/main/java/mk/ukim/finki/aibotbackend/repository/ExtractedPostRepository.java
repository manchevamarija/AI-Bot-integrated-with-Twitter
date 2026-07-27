package mk.ukim.finki.aibotbackend.repository;

import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExtractedPostRepository
    extends JpaRepository<ExtractedPost, Long>, JpaSpecificationExecutor<ExtractedPost> {
    List<ExtractedPost> findAllBySessionIdOrderByCreatedAtDesc(Long sessionId);
}

