package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.recommendation.CreateRecommendationRequest;
import tramplin.dto.recommendation.RecommendationResponse;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Opportunity;
import tramplin.entity.Recommendation;
import tramplin.entity.enums.ContactRequestStatus;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.exception.BusinessException;
import tramplin.exception.ConflictException;
import tramplin.entity.Company;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.CompanyRepository;
import tramplin.repository.ContactRequestRepository;
import tramplin.repository.OpportunityRepository;
import tramplin.repository.RecommendationRepository;
import tramplin.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final ApplicantProfileRepository applicantProfileRepository;
    private final OpportunityRepository opportunityRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public RecommendationResponse create(CreateRecommendationRequest request, UserPrincipal principal) {
        ApplicantProfile recommender = applicantProfileRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден"));

        ApplicantProfile recommended = applicantProfileRepository.findById(request.getRecommendedId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Рекомендуемый соискатель не найден: " + request.getRecommendedId()));

        if (recommender.getId().equals(recommended.getId())) {
            throw new BusinessException("SELF_RECOMMENDATION", "Нельзя рекомендовать самого себя");
        }

        UUID recommenderUserId = recommender.getUser().getId();
        UUID recommendedUserId = recommended.getUser().getId();
        boolean isContact = contactRequestRepository
                .findAllByUserIdAndStatus(recommenderUserId, ContactRequestStatus.ACCEPTED)
                .stream()
                .anyMatch(cr -> cr.getSender().getId().equals(recommendedUserId)
                        || cr.getReceiver().getId().equals(recommendedUserId));

        if (!isContact) {
            throw new BusinessException("NOT_CONTACT", "Рекомендовать можно только своего контакта");
        }

        Opportunity opportunity = opportunityRepository.findById(request.getOpportunityId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Вакансия не найдена: " + request.getOpportunityId()));

        if (opportunity.getStatus() != OpportunityStatus.ACTIVE) {
            throw new BusinessException("OPPORTUNITY_NOT_ACTIVE", "Вакансия не активна");
        }

        if (recommendationRepository.existsByRecommenderIdAndRecommendedIdAndOpportunityId(
                recommender.getId(), recommended.getId(), opportunity.getId())) {
            throw new ConflictException("Вы уже рекомендовали этого соискателя на эту вакансию");
        }

        Recommendation recommendation = Recommendation.builder()
                .recommender(recommender)
                .recommended(recommended)
                .opportunity(opportunity)
                .message(request.getMessage())
                .build();

        Recommendation saved = recommendationRepository.save(recommendation);
        log.info("Создана рекомендация: {} рекомендует {} на вакансию {}",
                recommenderUserId, recommendedUserId, opportunity.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getMy(UserPrincipal principal) {
        ApplicantProfile profile = findProfile(principal.getUserId());
        return recommendationRepository.findByRecommenderIdWithDetails(profile.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getForMe(UserPrincipal principal) {
        ApplicantProfile profile = findProfile(principal.getUserId());
        return recommendationRepository.findByRecommendedIdWithDetails(profile.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getByOpportunity(UUID opportunityId, UserPrincipal principal) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена: " + opportunityId));

        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Компания не найдена"));

        if (!opportunity.getEmployer().getId().equals(company.getId())) {
            throw new BusinessException("NOT_OWNER", "Вы можете просматривать рекомендации только на свои вакансии");
        }

        return recommendationRepository.findByOpportunityIdWithDetails(opportunityId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ApplicantProfile findProfile(UUID userId) {
        return applicantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден"));
    }

    private RecommendationResponse mapToResponse(Recommendation r) {
        return RecommendationResponse.builder()
                .id(r.getId())
                .recommenderName(r.getRecommender().getFirstName() + " " + r.getRecommender().getLastName())
                .recommendedName(r.getRecommended().getFirstName() + " " + r.getRecommended().getLastName())
                .opportunityTitle(r.getOpportunity().getTitle())
                .companyName(r.getOpportunity().getEmployer().getCompanyName())
                .message(r.getMessage())
                .createdAt(r.getCreatedAt())
                .build();
    }
}