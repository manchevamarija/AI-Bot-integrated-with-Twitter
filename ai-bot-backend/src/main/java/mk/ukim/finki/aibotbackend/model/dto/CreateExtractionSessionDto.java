package mk.ukim.finki.aibotbackend.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.enums.SocialNetwork;

public record CreateExtractionSessionDto(
    @NotNull
    SocialNetwork socialNetwork,
    @Size(max = 500)
    String description,
    @NotEmpty
    @Size(max = 20)
    List<@Valid CreateExtractionTargetDto> targets,
    @Min(1) @Max(100)
    Integer maxPosts,
    @DecimalMin("0.0") @DecimalMax("1.0")
    Double minMacedonianConfidence,
    Boolean includeText,
    Boolean includeImages,
    Boolean includeVideos
) {
    public CreateExtractionSessionDto(
        SocialNetwork socialNetwork,
        String description,
        List<CreateExtractionTargetDto> targets
    ) {
        this(socialNetwork, description, targets, 30, 0.0, true, true, true);
    }

    public ExtractionSession toExtractionSession() {
        ExtractionSession session = new ExtractionSession(socialNetwork, description);
        session.setMaxPosts(maxPosts == null ? 30 : maxPosts);
        session.setMinMacedonianConfidence(
            minMacedonianConfidence == null ? 0.0 : minMacedonianConfidence
        );
        session.setIncludeText(includeText == null || includeText);
        session.setIncludeImages(includeImages == null || includeImages);
        session.setIncludeVideos(includeVideos == null || includeVideos);
        targets
            .stream()
            .map(target -> target.toExtractionTarget(session))
            .forEach(session.getTargets()::add);
        return session;
    }
}
