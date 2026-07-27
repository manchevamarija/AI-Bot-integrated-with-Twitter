package mk.ukim.finki.aibotbackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionSession;
import mk.ukim.finki.aibotbackend.model.domain.ExtractionTarget;
import mk.ukim.finki.aibotbackend.model.enums.TargetType;

public record CreateExtractionTargetDto(
    @NotNull
    TargetType type,
    @NotBlank
    @Size(max = 200)
    String value
) {
    public ExtractionTarget toExtractionTarget(ExtractionSession session) {
        return new ExtractionTarget(type, value.trim(), session);
    }
}
