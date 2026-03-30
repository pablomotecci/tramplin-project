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
public class AssignmentPairDto {
    private UUID applicantId;
    private String applicantName;
    private UUID opportunityId;
    private String opportunityTitle;
    private String companyName;
    private double score;
}