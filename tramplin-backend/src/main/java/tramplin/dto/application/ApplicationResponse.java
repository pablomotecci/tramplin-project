package tramplin.dto.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;
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
    /**
     * User.id соискателя (не путать с ApplicantProfile.id).
     * По этому id фронт идёт в /profile/applicant/{userId}/* эндпоинты.
     * При маппинге всегда брать через app.getApplicant().getUser().getId().
     */
    private UUID applicantId;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;

    // Для работодателя — рекомендации контактов соискателя на эту вакансию.
    // Соискателю всегда пустой массив (не раскрываем его соц. граф).
    private List<RecommendationSummary> recommendations;

    // Для работодателя — совместимость соискателя с вакансией (прозрачность скоринга).
    // Соискателю — null (свой score он видит через отдельный scoring-endpoint).
    private ApplicationScoreSummary scoreSummary;
}
