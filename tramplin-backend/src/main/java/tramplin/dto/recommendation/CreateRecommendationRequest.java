package tramplin.dto.recommendation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRecommendationRequest {

    @NotNull
    private UUID recommendedId;

    @NotNull
    private UUID opportunityId;

    private String message;
}