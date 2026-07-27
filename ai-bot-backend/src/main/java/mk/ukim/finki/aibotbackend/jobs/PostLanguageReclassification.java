package mk.ukim.finki.aibotbackend.jobs;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import mk.ukim.finki.aibotbackend.bot.extraction.LanguageDetector;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Re-evaluates posts that have not entered a donation batch yet.
 *
 * <p>Language detection improves independently from the stored extraction data.
 * Reclassification keeps old, reviewable posts consistent with the current
 * detector without altering content that has already entered the donation
 * workflow.</p>
 */
@Component
@Slf4j
public class PostLanguageReclassification {
    private static final int PAGE_SIZE = 100;

    private final ExtractedPostService extractedPostService;
    private final LanguageDetector languageDetector;

    public PostLanguageReclassification(
        ExtractedPostService extractedPostService,
        LanguageDetector languageDetector
    ) {
        this.extractedPostService = extractedPostService;
        this.languageDetector = languageDetector;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reclassifyUndonatedPosts() {
        PostFilterDto undonated = new PostFilterDto(null, null, null, false, null);
        int pageNumber = 0;
        int updated = 0;

        while (true) {
            var page = extractedPostService.findAll(undonated, pageNumber, PAGE_SIZE);
            List<ExtractedPost> posts = page.getContent();

            for (ExtractedPost post : posts) {
                post.setMacedonianConfidence(
                    languageDetector.macedonianConfidence(post.getContent())
                );
            }
            if (!posts.isEmpty()) {
                extractedPostService.saveAll(posts);
                updated += posts.size();
            }

            if (page.isLast()) {
                break;
            }
            pageNumber++;
        }

        if (updated > 0) {
            log.info("Reclassified Macedonian confidence for {} undonated post(s)", updated);
        }
    }
}
