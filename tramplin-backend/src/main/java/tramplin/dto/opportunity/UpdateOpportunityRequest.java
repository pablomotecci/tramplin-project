package tramplin.dto.opportunity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.WorkFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOpportunityRequest {

    @Size(max = 255)
    private String title;

    private String description;

    private OpportunityType type;

    private WorkFormat workFormat;

    @Size(max = 100)
    private String city;

    @Size(max = 500)
    private String address;

    private Long salaryMin;

    private Long salaryMax;

    private LocalDateTime expiresAt;

    private LocalDateTime eventDate;

    @Email
    private String contactEmail;

    private String contactPhone;

    private String contactUrl;

    private List<UUID> tagIds;

    private List<String> mediaUrls;
}