package mk.ukim.finki.aibotbackend.service;

import jakarta.transaction.Transactional;
import java.util.List;
import mk.ukim.finki.aibotbackend.integration.vezilka.DonationReceipt;
import mk.ukim.finki.aibotbackend.integration.vezilka.VezilkaClient;
import mk.ukim.finki.aibotbackend.model.domain.ExtractedPost;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.enums.DonationStatus;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import mk.ukim.finki.aibotbackend.repository.ExtractedPostRepository;
import mk.ukim.finki.aibotbackend.repository.ExtractionSessionRepository;
import mk.ukim.finki.aibotbackend.service.domain.DonationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
public class DonationServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("aibot_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("llm.api-key", () -> "");
    }

    @Autowired private DonationService donationService;
    @Autowired private ExtractionSessionRepository sessionRepository;
    @Autowired private ExtractedPostRepository postRepository;
    @MockitoBean private VezilkaClient vezilkaClient;

    @Test
    void testDonationWorkflow() {
        ExtractionSession session = sessionRepository.save(new ExtractionSession(SocialNetwork.X, "test"));
        ExtractedPost post = postRepository.save(new ExtractedPost(
            session, "123", "finki", "Ова е македонски текст", "https://x.com/finki/status/123", null, 0.95
        ));
        org.mockito.Mockito.when(vezilkaClient.submitTextDonation(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new DonationReceipt("vezilka-123", "accepted", false, 0, null, "Accepted"));

        var draft = donationService.createBatch(List.of(post.getId()));
        org.assertj.core.api.Assertions.assertThat(draft.getStatus()).isEqualTo(DonationStatus.DRAFT);
        var approved = donationService.approve(draft.getId());
        org.assertj.core.api.Assertions.assertThat(approved.getStatus()).isEqualTo(DonationStatus.APPROVED);
        var submitted = donationService.submit(approved.getId());

        org.assertj.core.api.Assertions.assertThat(submitted.getStatus()).isEqualTo(DonationStatus.SUBMITTED);
        org.assertj.core.api.Assertions.assertThat(submitted.getVezilkaReference()).isEqualTo("vezilka-123");
        org.mockito.Mockito.verify(vezilkaClient).submitTextDonation(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void batchIsRejectedWhenVezilkaAcceptsNoneOfTheItems() {
        ExtractionSession session = sessionRepository.save(new ExtractionSession(SocialNetwork.X, "test"));
        ExtractedPost post = postRepository.save(new ExtractedPost(
            session, "124", "finki", "This is English, not Macedonian",
            "https://x.com/finki/status/124", null, 0.05
        ));
        org.mockito.Mockito.when(vezilkaClient.submitTextDonation(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new DonationReceipt("vezilka-124", "rejected", false, null, "not_macedonian", "Rejected"));

        var draft = donationService.createBatch(List.of(post.getId()));
        donationService.approve(draft.getId());
        var result = donationService.submit(draft.getId());

        org.assertj.core.api.Assertions.assertThat(result.getStatus()).isEqualTo(DonationStatus.REJECTED);
        org.assertj.core.api.Assertions.assertThat(result.getVezilkaReference()).isEqualTo("vezilka-124");
    }
}
