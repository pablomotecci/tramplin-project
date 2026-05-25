package tramplin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.request.ApplicantSearchRequest;
import tramplin.dto.response.ApplicantSearchResultResponse;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Tag;
import tramplin.entity.enums.TagCategory;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.specifications.ApplicantSpecifications;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicantSearchService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int TOP_TAGS_LIMIT = 3;

    /**
     * Приоритет категорий тегов в карточке поиска: чем меньше число — тем выше.
     * Логика — «техническая ДНК» соискателя: язык → фреймворк → область → БД → инструменты → уровень → тип занятости.
     * Уровень и тип занятости в самом конце — они задаются как фильтры, в карточке шумят.
     */
    private static final Map<TagCategory, Integer> CATEGORY_PRIORITY = Map.of(
            TagCategory.LANGUAGE, 1,
            TagCategory.FRAMEWORK, 2,
            TagCategory.SPECIALIZATION, 3,
            TagCategory.DATABASE, 4,
            TagCategory.TOOL, 5,
            TagCategory.LEVEL, 6,
            TagCategory.EMPLOYMENT_TYPE, 7
    );

    private static final Comparator<Tag> TAG_RANKING = Comparator
            .comparingInt((Tag t) -> CATEGORY_PRIORITY.getOrDefault(t.getCategory(), Integer.MAX_VALUE))
            .thenComparing(Tag::getName, Comparator.nullsLast(Comparator.naturalOrder()));

    private final ApplicantProfileRepository applicantProfileRepository;

    @Transactional(readOnly = true)
    public Page<ApplicantSearchResultResponse> search(
            ApplicantSearchRequest request,
            UUID viewerId,
            String viewerRole
    ) {
        Specification<ApplicantProfile> spec = Specification
                .where(ApplicantSpecifications.hasQueryInNameOrUniversity(request.getQuery()))
                .and(ApplicantSpecifications.hasAllTags(request.getTagIds()))
                .and(ApplicantSpecifications.hasUniversity(request.getUniversity()))
                .and(ApplicantSpecifications.graduationYearBetween(
                        request.getGraduationYearMin(),
                        request.getGraduationYearMax()))
                .and(ApplicantSpecifications.visibleTo(viewerId, viewerRole));

        Pageable pageable = PageRequest.of(
                safePage(request.getPage()),
                safeSize(request.getSize()),
                resolveSort(request.getSort())
        );

        return applicantProfileRepository.findAll(spec, pageable).map(this::toSearchResult);
    }

    private ApplicantSearchResultResponse toSearchResult(ApplicantProfile p) {
        List<String> topTags = p.getTags() == null ? List.of() : p.getTags().stream()
                .sorted(TAG_RANKING)
                .limit(TOP_TAGS_LIMIT)
                .map(Tag::getName)
                .toList();

        return ApplicantSearchResultResponse.builder()
                .userId(p.getUser().getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .avatarUrl(p.getAvatarUrl())
                .university(p.getUniversity())
                .graduationYear(p.getGraduationYear())
                .topTags(topTags)
                .build();
    }

    private Sort resolveSort(String sortKey) {
        if (sortKey == null) {
            return Sort.by("lastName", "firstName");
        }
        return switch (sortKey) {
            case "university" -> Sort.by("university").ascending();
            case "recent" -> Sort.by("updatedAt").descending();
            default -> Sort.by("lastName", "firstName");
        };
    }

    private int safePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int safeSize(Integer size) {
        if (size == null || size < 1) return DEFAULT_SIZE;
        return Math.min(size, MAX_SIZE);
    }
}