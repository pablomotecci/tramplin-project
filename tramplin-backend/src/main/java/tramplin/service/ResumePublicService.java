package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.resume.EducationResponse;
import tramplin.dto.resume.ExperienceResponse;
import tramplin.dto.resume.ProjectResponse;
import tramplin.dto.resume.PublicResumeResponse;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.PrivacySettings;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.PrivacySettingsRepository;
import tramplin.repository.ResumeEducationRepository;
import tramplin.repository.ResumeExperienceRepository;
import tramplin.repository.ResumeProjectRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumePublicService {

    private final ApplicantProfileRepository applicantProfileRepository;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final PrivacyService privacyService;

    private final ResumeExperienceRepository experienceRepository;
    private final ResumeProjectRepository projectRepository;
    private final ResumeEducationRepository educationRepository;

    /**
     * Публичный просмотр резюме другого соискателя.
     * Учитывает настройки приватности (resumeVisibility):
     *  - ALL: видно всем
     *  - EMPLOYERS_ONLY: только работодателям
     *  - CONTACTS_ONLY: только контактам
     *  - NOBODY: 404 «резюме скрыто»
     */
    @Transactional(readOnly = true)
    public PublicResumeResponse getPublicResume(UUID profileUserId, UUID viewerId, String viewerRole) {
        ApplicantProfile profile = applicantProfileRepository.findByUserId(profileUserId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден: " + profileUserId));

        PrivacySettings privacy = privacySettingsRepository.findByApplicantUserId(profileUserId)
                .orElse(null);

        // Если у соискателя настройки приватности — проверим доступ к резюме
        if (privacy != null && !privacyService.canView(
                privacy.getResumeVisibility(), viewerId, viewerRole, profileUserId)) {
            throw new EntityNotFoundException("Резюме скрыто настройками приватности");
        }

        List<ExperienceResponse> experiences = experienceRepository
                .findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::expToResponse)
                .toList();

        List<ProjectResponse> projects = projectRepository
                .findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::projToResponse)
                .toList();

        List<EducationResponse> education = educationRepository
                .findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::eduToResponse)
                .toList();

        return PublicResumeResponse.builder()
                .experiences(experiences)
                .projects(projects)
                .education(education)
                .build();
    }

    // Дублирующая маппинг-логика — компромисс ради изоляции read-only пути.
    // Можно вынести в общий маппер, но для МVP оставим тут.

    private ExperienceResponse expToResponse(tramplin.entity.ResumeExperience e) {
        return ExperienceResponse.builder()
                .id(e.getId())
                .organization(e.getOrganization())
                .position(e.getPosition())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .description(e.getDescription())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private ProjectResponse projToResponse(tramplin.entity.ResumeProject p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .role(p.getRole())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .description(p.getDescription())
                .projectUrl(p.getProjectUrl())
                .repositoryUrl(p.getRepositoryUrl())
                .displayOrder(p.getDisplayOrder())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private EducationResponse eduToResponse(tramplin.entity.ResumeEducation e) {
        return EducationResponse.builder()
                .id(e.getId())
                .institution(e.getInstitution())
                .faculty(e.getFaculty())
                .degree(e.getDegree().name())
                .startYear(e.getStartYear())
                .endYear(e.getEndYear())
                .description(e.getDescription())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}