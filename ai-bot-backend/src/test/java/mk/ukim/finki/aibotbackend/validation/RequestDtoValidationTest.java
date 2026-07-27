package mk.ukim.finki.aibotbackend.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractionSessionDto;
import mk.ukim.finki.aibotbackend.model.dto.CreateExtractionTargetDto;
import mk.ukim.finki.aibotbackend.model.dto.RegisterUserRequestDto;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;
import mk.ukim.finki.aibotbackend.model.enums.TargetType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RequestDtoValidationTest {
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsWeakRegistrationRequest() {
        var request = new RegisterUserRequestDto(
            "",
            "",
            "not-an-email",
            "a b",
            "123"
        );

        assertThat(validator.validate(request)).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void acceptsValidMacedonianTarget() {
        var request = new CreateExtractionSessionDto(
            SocialNetwork.X,
            "Македонска содржина",
            List.of(new CreateExtractionTargetDto(TargetType.KEYWORD, "Скопје"))
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsOversizedTarget() {
        var target = new CreateExtractionTargetDto(
            TargetType.KEYWORD,
            "а".repeat(201)
        );

        assertThat(validator.validate(target)).isNotEmpty();
    }

    @Test
    void rejectsInvalidExtractionOptions() {
        var request = new CreateExtractionSessionDto(
            SocialNetwork.X,
            "Options",
            List.of(new CreateExtractionTargetDto(TargetType.KEYWORD, "Скопје")),
            101,
            1.1,
            true,
            true,
            true
        );

        assertThat(validator.validate(request)).hasSize(2);
    }

    @Test
    void appliesSafeDefaultsForLegacyRequests() {
        var request = new CreateExtractionSessionDto(
            SocialNetwork.X,
            "Defaults",
            List.of(new CreateExtractionTargetDto(TargetType.KEYWORD, "Скопје"))
        );

        var session = request.toExtractionSession();

        assertThat(session.getMaxPosts()).isEqualTo(30);
        assertThat(session.getMinMacedonianConfidence()).isZero();
        assertThat(session.getIncludeText()).isTrue();
        assertThat(session.getIncludeImages()).isTrue();
        assertThat(session.getIncludeVideos()).isTrue();
    }
}
