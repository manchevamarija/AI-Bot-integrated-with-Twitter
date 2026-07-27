package mk.ukim.finki.aibotbackend.repository;

import jakarta.transaction.Transactional;
import mk.ukim.finki.aibotbackend.config.JpaConfig;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import mk.ukim.finki.aibotbackend.model.enums.TargetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Import(JpaConfig.class)
@Transactional
@Testcontainers
public class ExtractionSessionRepositoryTest {
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
    }

    @Autowired
    private ExtractionSessionRepository extractionSessionRepository;

    @Test
    void savesAndLoadsSessionWithXTargets() {
        ExtractionSession session = new ExtractionSession(SocialNetwork.X, "Macedonian hashtags");
        session.getTargets().add(new ExtractionTarget(TargetType.HASHTAG, "#Македонија", session));

        ExtractionSession saved = extractionSessionRepository.saveAndFlush(session);
        ExtractionSession loaded = extractionSessionRepository.findById(saved.getId()).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(loaded.getSocialNetwork()).isEqualTo(SocialNetwork.X);
        org.assertj.core.api.Assertions.assertThat(loaded.getTargets())
            .extracting(ExtractionTarget::getValue)
            .containsExactly("#Македонија");
    }
}
