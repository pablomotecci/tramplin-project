package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.application.ApplicationResponse;
import tramplin.dto.application.CreateApplicationRequest;
import tramplin.dto.application.UpdateApplicationStatusRequest;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Application;
import tramplin.entity.Company;
import tramplin.entity.Opportunity;
import tramplin.entity.enums.ApplicationStatus;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.exception.BusinessException;
import tramplin.exception.ConflictException;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.ApplicationRepository;
import tramplin.repository.CompanyRepository;
import tramplin.repository.OpportunityRepository;
import tramplin.security.UserPrincipal;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantProfileRepository applicantProfileRepository;
    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public ApplicationResponse createApplication(UserPrincipal principal, CreateApplicationRequest request) {
        ApplicantProfile applicant = applicantProfileRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + principal.getUserId()));

        Opportunity opportunity = opportunityRepository.findById(request.getOpportunityId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Вакансия не найдена: " + request.getOpportunityId()));

        if (opportunity.getStatus() != OpportunityStatus.ACTIVE) {
            throw new BusinessException("OPPORTUNITY_NOT_ACTIVE",
                    "Нельзя откликнуться на неактивную вакансию");
        }

        if (applicationRepository.existsByApplicantIdAndOpportunityId(applicant.getId(), opportunity.getId())) {
            throw new ConflictException("Вы уже откликнулись на эту вакансию");
        }

        Application application = Application.builder()
                .applicant(applicant)
                .opportunity(opportunity)
                .coverLetter(request.getCoverLetter())
                .build();

        Application saved = applicationRepository.save(application);
        log.info("Соискатель {} откликнулся на вакансию '{}'",
                applicant.getFirstName() + " " + applicant.getLastName(), opportunity.getTitle());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(UserPrincipal principal, Pageable pageable) {
        ApplicantProfile applicant = applicantProfileRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + principal.getUserId()));

        return applicationRepository.findByApplicantId(applicant.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getIncomingApplications(UserPrincipal principal, Pageable pageable,
                                                             ApplicationStatus status) {
        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        if (status != null) {
            return applicationRepository.findByOpportunityEmployerIdAndStatus(company.getId(), status, pageable)
                    .map(this::mapToResponse);
        }
        return applicationRepository.findByOpportunityEmployerId(company.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UserPrincipal principal, UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Отклик не найден: " + id));

        checkAccess(principal, application);
        return mapToResponse(application);
    }

    @Transactional
    public ApplicationResponse updateStatus(UserPrincipal principal, UUID id,
                                            UpdateApplicationStatusRequest request) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Отклик не найден: " + id));

        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        if (!application.getOpportunity().getEmployer().getId().equals(company.getId())) {
            throw new BusinessException("FORBIDDEN", "Вы не можете менять статус чужого отклика");
        }

        validateStatusTransition(application.getStatus(), request.getStatus());
        application.setStatus(request.getStatus());
        Application saved = applicationRepository.save(application);
        log.info("Статус отклика {} изменён на {} компанией {}",
                id, request.getStatus(), company.getCompanyName());
        return mapToResponse(saved);
    }

    private void validateStatusTransition(ApplicationStatus current, ApplicationStatus next) {
        if (next == ApplicationStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS",
                    "Нельзя вернуть отклик в статус PENDING");
        }
        if (current == ApplicationStatus.ACCEPTED || current == ApplicationStatus.REJECTED) {
            throw new BusinessException("INVALID_STATUS",
                    "Нельзя изменить финальный статус отклика");
        }
    }

    private void checkAccess(UserPrincipal principal, Application application) {
        boolean isApplicant = application.getApplicant().getUser().getId().equals(principal.getUserId());
        boolean isEmployer = application.getOpportunity().getEmployer().getUser().getId().equals(principal.getUserId());

        if (!isApplicant && !isEmployer) {
            throw new BusinessException("FORBIDDEN", "У вас нет доступа к этому отклику");
        }
    }

    private ApplicationResponse mapToResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .status(app.getStatus())
                .coverLetter(app.getCoverLetter())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .opportunityId(app.getOpportunity().getId())
                .opportunityTitle(app.getOpportunity().getTitle())
                .companyName(app.getOpportunity().getEmployer().getCompanyName())
                .applicantId(app.getApplicant().getId())
                .applicantFirstName(app.getApplicant().getFirstName())
                .applicantLastName(app.getApplicant().getLastName())
                .applicantEmail(app.getApplicant().getUser().getEmail())
                .build();
    }
}
