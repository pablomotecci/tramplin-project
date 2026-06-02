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
import tramplin.dto.application.RecommendationSummary;
import tramplin.dto.application.UpdateApplicationStatusRequest;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Application;
import tramplin.entity.Company;
import tramplin.entity.Opportunity;
import tramplin.entity.Recommendation;
import tramplin.entity.enums.ApplicationStatus;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.exception.BusinessException;
import tramplin.exception.ConflictException;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.ApplicationRepository;
import tramplin.repository.CompanyRepository;
import tramplin.repository.OpportunityRepository;
import tramplin.repository.RecommendationRepository;
import tramplin.security.UserPrincipal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantProfileRepository applicantProfileRepository;
    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;
    private final RecommendationRepository recommendationRepository;

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
        // Только что созданный отклик не может иметь рекомендаций.
        return mapToResponse(saved, Map.of());
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(UserPrincipal principal, Pageable pageable) {
        ApplicantProfile applicant = applicantProfileRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + principal.getUserId()));

        // Соискатель смотрит свои отклики — рекомендации ему не показываем (спойлер соц. графа).
        return applicationRepository.findByApplicantId(applicant.getId(), pageable)
                .map(app -> mapToResponse(app, Map.of()));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getIncomingApplications(UserPrincipal principal, Pageable pageable,
                                                             ApplicationStatus status) {
        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        Page<Application> page = (status != null)
                ? applicationRepository.findByOpportunityEmployerIdAndStatus(company.getId(), status, pageable)
                : applicationRepository.findByOpportunityEmployerId(company.getId(), pageable);

        // Один batch-запрос на всю страницу вместо N+1 по каждому отклику.
        Map<AppKey, List<RecommendationSummary>> recs = loadRecommendationsFor(page.getContent());
        return page.map(app -> mapToResponse(app, recs));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UserPrincipal principal, UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Отклик не найден: " + id));

        checkAccess(principal, application);

        // Рекомендации видит только работодатель, принимающий решение по отклику.
        // Соискателю их не показываем, чтобы не раскрывать факт рекомендации,
        // который рекомендующий мог хотеть оставить непубличным.
        boolean isEmployer = application.getOpportunity().getEmployer().getUser().getId()
                .equals(principal.getUserId());
        Map<AppKey, List<RecommendationSummary>> recs = isEmployer
                ? loadRecommendationsFor(List.of(application))
                : Map.of();
        return mapToResponse(application, recs);
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
        // Меняет статус всегда работодатель — показываем рекомендации.
        return mapToResponse(saved, loadRecommendationsFor(List.of(saved)));
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

    /**
     * Ключ для группировки рекомендаций по точной паре (соискатель, вакансия).
     * applicantId == Application.applicant.id == Recommendation.recommended.id.
     */
    private record AppKey(UUID applicantId, UUID opportunityId) {}

    /**
     * Batch-загрузка рекомендаций для всей страницы откликов: 1 SQL-запрос
     * вместо N+1. Возвращает Map по точным парам (applicantId, opportunityId).
     */
    private Map<AppKey, List<RecommendationSummary>> loadRecommendationsFor(List<Application> apps) {
        if (apps.isEmpty()) {
            return Map.of();
        }

        Set<UUID> applicantIds = apps.stream()
                .map(a -> a.getApplicant().getId())
                .collect(Collectors.toSet());
        Set<UUID> opportunityIds = apps.stream()
                .map(a -> a.getOpportunity().getId())
                .collect(Collectors.toSet());

        List<Recommendation> all = recommendationRepository.findForApplicationBatch(applicantIds, opportunityIds);

        // Группируем по точным парам (а не по декартову произведению из запроса).
        return all.stream()
                .collect(Collectors.groupingBy(
                        r -> new AppKey(r.getRecommended().getId(), r.getOpportunity().getId()),
                        Collectors.mapping(this::toSummary, Collectors.toList())
                ));
    }

    private RecommendationSummary toSummary(Recommendation r) {
        ApplicantProfile recommender = r.getRecommender();
        return RecommendationSummary.builder()
                .recommenderUserId(recommender.getUser().getId())
                .recommenderName(recommender.getFirstName() + " " + recommender.getLastName())
                .recommenderAvatarUrl(recommender.getAvatarUrl())
                .message(r.getMessage())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ApplicationResponse mapToResponse(Application app,
                                             Map<AppKey, List<RecommendationSummary>> recommendations) {
        AppKey key = new AppKey(app.getApplicant().getId(), app.getOpportunity().getId());
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
                // Всегда массив (возможно пустой) — стандарт наших DTO, фронту удобнее.
                .recommendations(recommendations.getOrDefault(key, List.of()))
                .build();
    }
}
