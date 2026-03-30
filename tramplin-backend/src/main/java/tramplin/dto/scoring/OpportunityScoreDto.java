package tramplin.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityScoreDto {
    private UUID opportunityId;
    private String title;
    private String companyName;
    private double score;
}