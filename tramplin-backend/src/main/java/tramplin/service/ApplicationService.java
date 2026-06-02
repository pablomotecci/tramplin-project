package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.application.ApplicationResponse;
import tramplin.dto.application.ApplicationScoreSummary;
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
    private final ScoringService scoringService;

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
        // Только что созданный отклик: ни рекомендаций, ни score для работодателя ещё нет.
        return mapToResponse(saved, AppEnrichment.empty());
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getMyApplications(UserPrincipal principal, Pageable pageable) {
        ApplicantProfile applicant = applicantProfileRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + principal.getUserId()));

        // Соискатель смотрит свои отклики — ни рекомендаций (спойлер соц. графа),
        // ни score (его он видит через scoring-endpoint) ему здесь не показываем.
        return applicationRepository.findByApplicantId(applicant.getId(), pageable)
                .map(app -> mapToResponse(app, AppEnrichment.empty()));
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

        // Batch на всю страницу: рекомендации + score, без N+1.
        AppEnrichment enrichment = enrichmentFor(page.getContent());
        return page.map(app -> mapToResponse(app, enrichment));
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UserPrincipal principal, UUID id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Отклик не найден: " + id));

        checkAccess(principal, application);

        // Рекомендации и score видит только работодатель, принимающий решение по отклику.
        // Соискателю их не показываем: рекомендации — чтобы не раскрывать факт
        // рекомендации (соц. граф), score — он доступен ему через scoring-endpoint.
        boolean isEmployer = application.getOpportunity().getEmployer().getUser().getId()
                .equals(principal.getUserId());
        AppEnrichment enrichment = isEmployer
                ? enrichmentFor(List.of(application))
                : AppEnrichment.empty();
        return mapToResponse(application, enrichment);
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
        // Меняет статус всегда работодатель — показываем рекомендации и score.
        return mapToResponse(saved, enrichmentFor(List.of(saved)));
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
     * Контейнер обогащения страницы откликов: рекомендации (ключ по ApplicantProfile.id)
     * и score-совместимость (ключ по User.id). Две разные мапы с разными ключами.
     */
    private record AppEnrichment(
            Map<AppKey, List<RecommendationSummary>> recommendations,
            Map<ScoringService.ApplicationScoreKey, ApplicationScoreSummary> scores
    ) {
        static AppEnrichment empty() {
            return new AppEnrichment(Map.of(), Map.of());
        }
    }

    /** Загружает рекомендации и score одним batch'ем для пачки откликов (вид работодателя). */
    private AppEnrichment enrichmentFor(List<Application> apps) {
        return new AppEnrichment(loadRecommendationsFor(apps), loadScoresFor(apps));
    }

    private Map<ScoringService.ApplicationScoreKey, ApplicationScoreSummary> loadScoresFor(List<Application> apps) {
        if (apps.isEmpty()) {
            return Map.of();
        }
        Set<ScoringService.ApplicationScoreKey> keys = apps.stream()
                .map(a -> new ScoringService.ApplicationScoreKey(
                        a.getApplicant().getUser().getId(), a.getOpportunity().getId()))
                .collect(Collectors.toSet());
        return scoringService.calculateBatch(keys);
    }

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

    private ApplicationResponse mapToResponse(Application app, AppEnrichment enrichment) {
        AppKey recKey = new AppKey(app.getApplicant().getId(), app.getOpportunity().getId());
        ScoringService.ApplicationScoreKey scoreKey = new ScoringService.ApplicationScoreKey(
                app.getApplicant().getUser().getId(), app.getOpportunity().getId());
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
                .recommendations(enrichment.recommendations().getOrDefault(recKey, List.of()))
                // score: null для соискателя/нового отклика, заполнено для работодателя.
                .scoreSummary(enrichment.scores().get(scoreKey))
                .build();
    }
}
