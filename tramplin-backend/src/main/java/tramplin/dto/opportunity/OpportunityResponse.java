package tramplin.dto.opportunity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.WorkFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityResponse {

    private UUID id;

    private String title;
    private String description;
    private OpportunityType type;
    private WorkFormat workFormat;
    private OpportunityStatus status;

    private String city;
    private String address;
    private Double latitude;
    private Double longitude;

    private Long salaryMin;
    private Long salaryMax;

    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime eventDate;

    private String contactEmail;
    private String contactPhone;
    private String contactUrl;

    private UUID employerId;
    private String companyName;
    private String logoUrl;

    private List<String> tags;

    private List<String> mediaUrls;
}