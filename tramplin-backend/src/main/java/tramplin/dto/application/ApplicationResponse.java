package tramplin.dto.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private UUID id;
    private ApplicationStatus status;
    private String coverLetter;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Для соискателя — что за вакансия и компания
    private UUID opportunityId;
    private String opportunityTitle;
    private String companyName;

    // Для работодателя — кто откликнулся
    private UUID applicantId;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;
}
