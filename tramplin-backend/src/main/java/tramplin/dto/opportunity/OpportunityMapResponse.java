package tramplin.dto.opportunity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.WorkFormat;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityMapResponse {

    private UUID id;
    private String title;
    private String companyName;
    private String logoUrl;
    private OpportunityType type;
    private WorkFormat workFormat;
    private String city;
    private Double latitude;
    private Double longitude;
    private Long salaryMin;
    private Long salaryMax;
    private List<String> tags;
}