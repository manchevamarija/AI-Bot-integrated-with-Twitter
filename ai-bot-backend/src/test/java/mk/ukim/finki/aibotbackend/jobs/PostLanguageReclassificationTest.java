package mk.ukim.finki.aibotbackend.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import mk.ukim.finki.aibotbackend.bot.extraction.LanguageDetector;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.dto.PostFilterDto;
import mk.ukim.finki.aibotbackend.service.domain.ExtractedPostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class PostLanguageReclassificationTest {
    @Mock
    private ExtractedPostService extractedPostService;

    @Mock
    private LanguageDetector languageDetector;

    @Test
    void recalculatesAndPersistsUndonatedPostScores() {
        ExtractedPost russianPost = new ExtractedPost();
        russianPost.setContent("Мне нравится что взяли карту и союзники");
        russianPost.setMacedonianConfidence(0.75);

        when(extractedPostService.findAll(any(PostFilterDto.class), eq(0), eq(100)))
            .thenReturn(new PageImpl<>(List.of(russianPost)));
        when(languageDetector.macedonianConfidence(russianPost.getContent()))
            .thenReturn(0.15);

        new PostLanguageReclassification(extractedPostService, languageDetector)
            .reclassifyUndonatedPosts();

        assertThat(russianPost.getMacedonianConfidence()).isEqualTo(0.15);
        verify(extractedPostService).saveAll(List.of(russianPost));
    }

    @Test
    void doesNotWriteWhenThereAreNoUndonatedPosts() {
        when(extractedPostService.findAll(any(PostFilterDto.class), eq(0), eq(100)))
            .thenReturn(new PageImpl<>(List.of()));

        new PostLanguageReclassification(extractedPostService, languageDetector)
            .reclassifyUndonatedPosts();

        verify(extractedPostService, never()).saveAll(any());
    }
}
