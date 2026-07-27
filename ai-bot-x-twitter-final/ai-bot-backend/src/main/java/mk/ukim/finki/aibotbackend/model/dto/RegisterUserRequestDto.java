package mk.ukim.finki.aibotbackend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import mk.ukim.finki.aibotbackend.model.domain.User;

public record RegisterUserRequestDto(
    @NotBlank @Size(max = 80) String name,
    @NotBlank @Size(max = 80) String surname,
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[\\p{L}\\p{N}._-]+$")
    String username,
    @NotBlank @Size(min = 8, max = 128) String password
) {
    public User toUser() {
        return new User(
            name.trim(),
            surname.trim(),
            email.trim().toLowerCase(),
            username.trim(),
            password
        );
    }
}
