package mk.ukim.finki.aibotbackend.service.application;

import java.util.Optional;
import java.util.List;
import mk.ukim.finki.aibotbackend.model.dto.DisplayExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import mk.ukim.finki.aibotbackend.model.dto.SessionStatisticsDto;
import mk.ukim.finki.aibotbackend.model.enums.TopPostMetric;
import org.springframework.data.domain.Page;

/**
 * Application service for browsing the extracted content.
 */
public interface ExtractedPostApplicationService {
    Page<DisplayExtractedPostDto> findAll(PostFilterDto filter, int page, int size);

    Optional<DisplayExtractedPostDto> findById(Long id);

    Optional<DisplayExtractedPostDto> deleteById(Long id);

    Optional<byte[]> loadVideo(Long postId, Long mediaId);

    List<DisplayExtractedPostDto> findAllBySessionId(Long sessionId);

    SessionStatisticsDto statistics(Long sessionId);

    List<DisplayExtractedPostDto> findTop(PostFilterDto filter, TopPostMetric metric, int limit);
}
