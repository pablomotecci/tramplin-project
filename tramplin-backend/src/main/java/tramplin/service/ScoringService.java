package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.application.ApplicationScoreSummary;
import tramplin.dto.scoring.ApplicantScoreDto;
import tramplin.dto.scoring.OpportunityScoreDto;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Opportunity;
import tramplin.entity.Tag;
import tramplin.entity.enums.TagCategory;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.OpportunityRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * Делегат-обёртка для сценариев, которым нужно только число
     * (ранжирование, рекомендации, венгерский алгоритм). Поведение тождественно
     * прежнему computeScore — детали матчей просто отбрасываются.
     */
    private double computeScore(Set<Tag> applicantTags, Set<Tag> opportunityTags) {
        return computeScoreWithDetails(applicantTags, opportunityTags).score();
    }

    public double computeScorePublic(Set<Tag> applicantTags, Set<Tag> opportunityTags) {
        return computeScore(applicantTags, opportunityTags);
    }

    /**
     * Движок скоринга с сохранением «прозрачности»: помимо итогового score
     * собирает по каждому требуемому тегу вакансии лучший матч и его тип.
     * Теги без совпадения (NONE) в matches не попадают.
     */
    ScoreResult computeScoreWithDetails(Set<Tag> applicantTags, Set<Tag> opportunityTags) {
        if (opportunityTags.isEmpty()) {
            return new ScoreResult(0.0, List.of());
        }

        double weightedSum = 0.0;
        double totalWeight = 0.0;
        List<TagMatchDetail> matches = new ArrayList<>();

        for (Tag required : opportunityTags) {
            int weight = CATEGORY_WEIGHTS.getOrDefault(required.getCategory(), DEFAULT_WEIGHT);
            MatchType bestType = MatchType.NONE;

            for (Tag skill : applicantTags) {
                MatchType type = matchType(skill, required);
                // Сравниваем по приоритету, а не по весу: EXACT и SYNONYM имеют
                // одинаковый вес 1.0, но при наличии обоих в подписи должен победить
                // EXACT (иначе работодатель увидит «синоним», хотя точное совпадение есть).
                if (matchPriority(type) > matchPriority(bestType)) {
                    bestType = type;
                }
                if (bestType == MatchType.EXACT) {
                    break; // выше приоритета нет — дальше искать незачем
                }
            }

            weightedSum += weight * matchWeight(bestType);
            totalWeight += weight;

            if (bestType != MatchType.NONE) {
                matches.add(new TagMatchDetail(required, bestType));
            }
        }

        return new ScoreResult(weightedSum / totalWeight, matches);
    }

    private MatchType matchType(Tag skill, Tag required) {
        if (skill.getId().equals(required.getId())) {
            return MatchType.EXACT;
        }

        if (isSynonym(skill, required) || isSynonym(required, skill)) {
            return MatchType.SYNONYM;
        }

        if (skill.getParent() != null && required.getParent() != null
                && skill.getParent().getId().equals(required.getParent().getId())) {
            return MatchType.SIBLING;
        }

        if (skill.getParent() != null && skill.getParent().getId().equals(required.getId())) {
            return MatchType.HIERARCHY;
        }

        if (required.getParent() != null && required.getParent().getId().equals(skill.getId())) {
            return MatchType.HIERARCHY;
        }

        return MatchType.NONE;
    }

    /**
     * Вес совпадения для расчёта score (число). EXACT и SYNONYM эквивалентны —
     * синоним так же хорош, как точное попадание.
     */
    private double matchWeight(MatchType type) {
        return switch (type) {
            case EXACT, SYNONYM -> EXACT_MATCH;   // 1.0
            case SIBLING -> SIBLING_MATCH;         // 0.7
            case HIERARCHY -> HIERARCHY_MATCH;     // 0.5
            case NONE -> NO_MATCH;                 // 0.0
        };
    }

    /**
     * Приоритет типа для подписи в matchedTags (отображение, НЕ score).
     * Отделён от matchWeight намеренно: при равном весе EXACT (4) важнее
     * SYNONYM (3), чтобы UI не показывал «синоним» там, где есть точное совпадение.
     */
    private int matchPriority(MatchType type) {
        return switch (type) {
            case EXACT -> 4;
            case SYNONYM -> 3;
            case SIBLING -> 2;
            case HIERARCHY -> 1;
            case NONE -> 0;
        };
    }

    private boolean isSynonym(Tag a, Tag b) {
        return a.getSynonyms().stream()
                .anyMatch(s -> s.getSynonym().equalsIgnoreCase(b.getName()));
    }

    // ---- Прозрачность скоринга (B7a) ----

    /**
     * Внутреннее представление результата с деталями. Держит Entity-теги,
     * маппинг в строковый DTO MatchedTag делается отдельно.
     */
    record ScoreResult(double score, List<TagMatchDetail> matches) {}

    record TagMatchDetail(Tag matchedTag, MatchType matchType) {}

    /**
     * Ключ batch-расчёта совместимости. applicantUserId — это User.id
     * (не путать с ApplicantProfile.id из B16).
     */
    public record ApplicationScoreKey(UUID applicantUserId, UUID opportunityId) {}

    /**
     * Совместимость одной пары (соискатель, вакансия) с деталями матчей.
     * Для точечного открытия отклика работодателем.
     */
    @Transactional(readOnly = true)
    public ApplicationScoreSummary getScoreSummary(UUID applicantUserId, UUID opportunityId) {
        ApplicantProfile profile = applicantProfileRepository.findByUserIdWithTags(applicantUserId)
                .orElseThrow(() -> new EntityNotFoundException("Профиль соискателя не найден: " + applicantUserId));
        Opportunity opportunity = opportunityRepository.findByIdWithTagsAndEmployer(opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Возможность не найдена: " + opportunityId));

        ScoreResult result = computeScoreWithDetails(profile.getTags(), opportunity.getTags());
        return toSummary(result, opportunity.getTags().size());
    }

    /**
     * Batch-расчёт совместимости для страницы откликов: 2 SQL-запроса
     * (профили + вакансии) вместо 2×N, дальше всё в памяти.
     */
    @Transactional(readOnly = true)
    public Map<ApplicationScoreKey, ApplicationScoreSummary> calculateBatch(Set<ApplicationScoreKey> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }

        Set<UUID> applicantUserIds = keys.stream()
                .map(ApplicationScoreKey::applicantUserId)
                .collect(Collectors.toSet());
        Set<UUID> opportunityIds = keys.stream()
                .map(ApplicationScoreKey::opportunityId)
                .collect(Collectors.toSet());

        Map<UUID, ApplicantProfile> profiles = applicantProfileRepository
                .findAllByUserIdInWithTags(applicantUserIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
        Map<UUID, Opportunity> opportunities = opportunityRepository
                .findAllByIdInWithTags(opportunityIds).stream()
                .collect(Collectors.toMap(Opportunity::getId, o -> o));

        Map<ApplicationScoreKey, ApplicationScoreSummary> result = new HashMap<>();
        for (ApplicationScoreKey key : keys) {
            ApplicantProfile profile = profiles.get(key.applicantUserId());
            Opportunity opportunity = opportunities.get(key.opportunityId());
            if (profile == null || opportunity == null) {
                continue; // отсутствующие пары → в мапу не кладём, фронт получит null
            }
            ScoreResult sr = computeScoreWithDetails(profile.getTags(), opportunity.getTags());
            result.put(key, toSummary(sr, opportunity.getTags().size()));
        }
        return result;
    }

    private ApplicationScoreSummary toSummary(ScoreResult result, int totalRequiredTags) {
        List<ApplicationScoreSummary.MatchedTag> matchedTags = result.matches().stream()
                .map(this::toMatchedTag)
                .toList();
        return ApplicationScoreSummary.builder()
                .scorePercent((int) Math.round(result.score() * 100))
                .matchedTags(matchedTags)
                .totalRequiredTags(totalRequiredTags)
                .build();
    }

    private ApplicationScoreSummary.MatchedTag toMatchedTag(TagMatchDetail detail) {
        Tag tag = detail.matchedTag();
        return new ApplicationScoreSummary.MatchedTag(
                tag.getName(),
                tag.getCategory().name(),
                detail.matchType().name()
        );
    }
}