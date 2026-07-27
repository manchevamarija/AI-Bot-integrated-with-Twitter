package mk.ukim.finki.aibotbackend.service.application.impl;

import java.net.URI;
import java.util.Optional;
import java.util.List;
import mk.ukim.finki.aibotbackend.model.dto.DisplayExtractedPostDto;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import mk.ukim.finki.aibotbackend.model.dto.SessionStatisticsDto;
import mk.ukim.finki.aibotbackend.model.enums.MediaType;
import mk.ukim.finki.aibotbackend.service.application.ExtractedPostApplicationService;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class ExtractedPostApplicationServiceImpl implements ExtractedPostApplicationService {
    private final ExtractedPostService extractedPostService;

    public ExtractedPostApplicationServiceImpl(ExtractedPostService extractedPostService) {
        this.extractedPostService = extractedPostService;
    }

    @Override
    public Page<DisplayExtractedPostDto> findAll(PostFilterDto filter, int page, int size) {
        return extractedPostService.findAll(filter,page,size).map(DisplayExtractedPostDto::from);
    }

    @Override
    public Optional<DisplayExtractedPostDto> findById(Long id) {
        return extractedPostService.findById(id).map(DisplayExtractedPostDto::from);
    }

    @Override
    public Optional<DisplayExtractedPostDto> deleteById(Long id) {
        return extractedPostService.deleteById(id).map(DisplayExtractedPostDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> loadVideo(Long postId, Long mediaId) {
        return extractedPostService.findById(postId)
            .flatMap(post -> post.getMediaItems().stream()
                .filter(media -> media.getId().equals(mediaId))
                .filter(media -> media.getType() == MediaType.VIDEO)
                .findFirst())
            .map(media -> URI.create(media.getSourceUrl()))
            .filter(uri -> "https".equalsIgnoreCase(uri.getScheme()))
            .filter(uri -> "video.twimg.com".equalsIgnoreCase(uri.getHost()))
            .map(uri -> RestClient.create()
                .get()
                .uri(uri)
                .retrieve()
                .body(byte[].class));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisplayExtractedPostDto> findAllBySessionId(Long sessionId) {
        return DisplayExtractedPostDto.from(extractedPostService.findAllBySessionId(sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public SessionStatisticsDto statistics(Long sessionId) {
        var posts = extractedPostService.findAllBySessionId(sessionId);
        long macedonian = posts.stream()
            .filter(post -> post.getMacedonianConfidence() != null)
            .filter(post -> post.getMacedonianConfidence() >= 0.5)
            .count();
        long withMedia = posts.stream().filter(post -> !post.getMediaItems().isEmpty()).count();
        long withImages = posts.stream()
            .filter(post -> post.getMediaItems().stream().anyMatch(media -> media.getType() == MediaType.IMAGE))
            .count();
        long withVideos = posts.stream()
            .filter(post -> post.getMediaItems().stream().anyMatch(media -> media.getType() == MediaType.VIDEO))
            .count();
        long donated = posts.stream().filter(post -> post.getDonationBatch() != null).count();
        return new SessionStatisticsDto(
            posts.size(),
            macedonian,
            withMedia,
            withImages,
            withVideos,
            donated
        );
    }
}
