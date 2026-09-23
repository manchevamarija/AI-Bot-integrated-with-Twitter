package mk.ukim.finki.aibotbackend.service.domain;

import java.util.List;
import java.util.Optional;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import org.springframework.data.domain.Page;

/**
 * Domain service for extracted posts.
 */
public interface ExtractedPostService {
    /**
     * Paged browsing of extracted posts. Every filter field may be
     * {@code null} — see {@link PostFilterDto}.
     */
    Page<ExtractedPost> findAll(PostFilterDto filter, int page, int size);

    Optional<ExtractedPost> findById(Long id);

    List<ExtractedPost> findAllById(List<Long> ids);

    List<ExtractedPost> findAllBySessionId(Long sessionId);

    /**
     * Persists the posts a bot run produced.
     */
    List<ExtractedPost> saveAll(List<ExtractedPost> posts);

    Optional<ExtractedPost> deleteById(Long id);

    /**
     * The highest-ranked posts by the given persisted numeric attribute,
     * skipping posts where that counter is unknown.
     */
    List<ExtractedPost> findTop(PostFilterDto filter, String attribute, int limit);
}
