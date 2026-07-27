package mk.ukim.finki.aibotbackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginUserRequestDto(
    @NotBlank @Size(max = 50) String username,
    @NotBlank @Size(max = 128) String password
) {
}
