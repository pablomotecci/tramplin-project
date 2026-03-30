package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.scoring.ApplicantScoreDto;
import tramplin.dto.scoring.OpportunityScoreDto;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Opportunity;
import tramplin.entity.Tag;
import tramplin.entity.enums.TagCategory;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.OpportunityRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private static final Map<TagCategory, Integer> CATEGORY_WEIGHTS = Map.of(
            TagCategory.LANGUAGE, 3,
            TagCategory.FRAMEWORK, 2,
            TagCategory.LEVEL, 2,
            TagCategory.EMPLOYMENT_TYPE, 1,
            TagCategory.SPECIALIZATION, 1,
            TagCategory.TOOL, 1,
            TagCategory.DATABASE, 1
    );

    private static final int DEFAULT_WEIGHT = 1;

    private static final double EXACT_MATCH = 1.0;
    private static final double SYNONYM_MATCH = 1.0;
    private static final double SIBLING_MATCH = 0.7;
    private static final double HIERARCHY_MATCH = 0.5;
    private static final double NO_MATCH = 0.0;

    private final ApplicantProfileRepository applicantProfileRepository;
    private final OpportunityRepository opportunityRepository;

    @Transactional(readOnly = true)
    public double calculateScore(UUID applicantId, UUID opportunityId) {
        ApplicantProfile profile = applicantProfileRepository.findByUserIdWithTags(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден: " + applicantId));

        Opportunity opportunity = opportunityRepository.findByIdWithTagsAndEmployer(opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Возможность не найдена: " + opportunityId));

        return computeScore(profile.getTags(), opportunity.getTags());
    }

    @Transactional(readOnly = true)
    public List<ApplicantScoreDto> calculateScoresForOpportunity(UUID opportunityId, UUID requesterId, String requesterRole) {
        Opportunity opportunity = opportunityRepository.findByIdWithTagsAndEmployer(opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Возможность не найдена: " + opportunityId));

        if ("EMPLOYER".equals(requesterRole)
                && !opportunity.getEmployer().getUser().getId().equals(requesterId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Нет доступа к ранжированию кандидатов для чужой возможности");
        }

        Set<Tag> opportunityTags = opportunity.getTags();

        return applicantProfileRepository.findAllWithTagsAndUser().stream()
                .map(profile -> {
                    double score = computeScore(profile.getTags(), opportunityTags);
                    return ApplicantScoreDto.builder()
                            .userId(profile.getUser().getId())
                            .displayName(profile.getUser().getDisplayName())
                            .score(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(ApplicantScoreDto::getScore).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OpportunityScoreDto> calculateScoresForApplicant(UUID applicantId) {
        ApplicantProfile profile = applicantProfileRepository.findByUserIdWithTags(applicantId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден: " + applicantId));

        Set<Tag> applicantTags = profile.getTags();

        return opportunityRepository.findAllActiveWithTagsAndEmployer().stream()
                .map(opportunity -> {
                    double score = computeScore(applicantTags, opportunity.getTags());
                    return OpportunityScoreDto.builder()
                            .opportunityId(opportunity.getId())
                            .title(opportunity.getTitle())
                            .companyName(opportunity.getEmployer().getCompanyName())
                            .score(score)
                            .build();
                })
                .sorted(Comparator.comparingDouble(OpportunityScoreDto::getScore).reversed())
                .toList();
    }

    private double computeScore(Set<Tag> applicantTags, Set<Tag> opportunityTags) {
        if (opportunityTags.isEmpty()) {
            return 0.0;
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Tag required : opportunityTags) {
            int weight = CATEGORY_WEIGHTS.getOrDefault(required.getCategory(), DEFAULT_WEIGHT);
            double bestMatch = NO_MATCH;

            for (Tag skill : applicantTags) {
                double match = matchTags(skill, required);
                if (match > bestMatch) {
                    bestMatch = match;
                }
                if (bestMatch == EXACT_MATCH) {
                    break;
                }
            }

            weightedSum += weight * bestMatch;
            totalWeight += weight;
        }

        return weightedSum / totalWeight;
    }

    public double computeScorePublic(Set<Tag> applicantTags, Set<Tag> opportunityTags) {
        return computeScore(applicantTags, opportunityTags);
    }

    private double matchTags(Tag skill, Tag required) {
        if (skill.getId().equals(required.getId())) {
            return EXACT_MATCH;
        }

        if (isSynonym(skill, required) || isSynonym(required, skill)) {
            return SYNONYM_MATCH;
        }

        if (skill.getParent() != null && required.getParent() != null
                && skill.getParent().getId().equals(required.getParent().getId())) {
            return SIBLING_MATCH;
        }

        if (skill.getParent() != null && skill.getParent().getId().equals(required.getId())) {
            return HIERARCHY_MATCH;
        }

        if (required.getParent() != null && required.getParent().getId().equals(skill.getId())) {
            return HIERARCHY_MATCH;
        }

        return NO_MATCH;
    }

    private boolean isSynonym(Tag a, Tag b) {
        return a.getSynonyms().stream()
                .anyMatch(s -> s.getSynonym().equalsIgnoreCase(b.getName()));
    }
}