package tramplin.dto.opportunity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateOpportunityRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private OpportunityType type;

    @NotNull
    private WorkFormat workFormat;

    @NotBlank
    private String city;

    private String address;

    private Long salaryMin;

    private Long salaryMax;

    private LocalDateTime expiresAt;

    private LocalDateTime eventDate;

    @NotBlank
    @Email
    private String contactEmail;

    private String contactPhone;

    private String contactUrl;

    private List<UUID> tagIds;

    private List<String> mediaUrls;
}