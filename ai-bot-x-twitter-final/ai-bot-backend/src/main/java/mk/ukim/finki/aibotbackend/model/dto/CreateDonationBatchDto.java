package mk.ukim.finki.aibotbackend.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request for grouping already-extracted posts into a donation batch.
 */
public record CreateDonationBatchDto(
    @NotEmpty
    @Size(max = 100)
    List<@NotNull @Positive Long> postIds
) {
}
