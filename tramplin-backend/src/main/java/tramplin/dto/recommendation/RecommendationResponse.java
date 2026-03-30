package tramplin.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponse {

    private UUID id;
    private String recommenderName;
    private String recommendedName;
    private String opportunityTitle;
    private String companyName;
    private String message;
    private LocalDateTime createdAt;
}