package tramplin.dto.scoring;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResultDto {
    private List<AssignmentPairDto> pairs;
    private double totalScore;
    private int applicantCount;
    private int opportunityCount;
}