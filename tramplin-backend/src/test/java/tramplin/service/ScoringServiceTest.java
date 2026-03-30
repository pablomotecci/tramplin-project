package tramplin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tramplin.entity.Tag;
import tramplin.entity.enums.TagCategory;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.OpportunityRepository;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для ScoringService.
 *
 * Тестируем ядро скоринга — метод computeScorePublic(),
 * который рассчитывает совместимость тегов соискателя и вакансии.
 * Используем Mockito для мокирования репозиториев (они не нужны
 * для тестирования чистой логики скоринга).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringService — расчёт совместимости тегов")
class ScoringServiceTest {

    @Mock
    private ApplicantProfileRepository applicantProfileRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @InjectMocks
    private ScoringService scoringService;

    @Test
    @DisplayName("computeScore — все теги совпадают → score = 1.0")
    void computeScore_whenAllTagsMatch_thenReturnsOne() {
        Tag java = createTag("Java", TagCategory.LANGUAGE);
        Tag spring = createTag("Spring", TagCategory.FRAMEWORK);

        Set<Tag> applicantTags = Set.of(java, spring);
        Set<Tag> opportunityTags = Set.of(java, spring);

        double score = scoringService.computeScorePublic(applicantTags, opportunityTags);

        assertThat(score).isEqualTo(1.0);
    }

    @Test
    @DisplayName("computeScore — частичное совпадение → score между 0 и 1")
    void computeScore_whenPartialMatch_thenReturnsBetweenZeroAndOne() {
        Tag java = createTag("Java", TagCategory.LANGUAGE);
        Tag spring = createTag("Spring", TagCategory.FRAMEWORK);
        Tag python = createTag("Python", TagCategory.LANGUAGE);

        Set<Tag> applicantTags = Set.of(java);
        Set<Tag> opportunityTags = Set.of(java, spring, python);

        double score = scoringService.computeScorePublic(applicantTags, opportunityTags);

        assertThat(score).isGreaterThan(0.0).isLessThan(1.0);
    }


    @Test
    @DisplayName("computeScore — нет совпадений → score = 0.0")
    void computeScore_whenNoMatch_thenReturnsZero() {
        Tag java = createTag("Java", TagCategory.LANGUAGE);
        Tag python = createTag("Python", TagCategory.LANGUAGE);

        Set<Tag> applicantTags = Set.of(java);
        Set<Tag> opportunityTags = Set.of(python);

        double score = scoringService.computeScorePublic(applicantTags, opportunityTags);

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    @DisplayName("computeScore — пустые теги вакансии → score = 0.0")
    void computeScore_whenOpportunityTagsEmpty_thenReturnsZero() {
        Tag java = createTag("Java", TagCategory.LANGUAGE);

        double score = scoringService.computeScorePublic(Set.of(java), Set.of());

        assertThat(score).isEqualTo(0.0);
    }


    @Test
    @DisplayName("computeScore — sibling-теги (общий parent) → score = 0.7")
    void computeScore_whenSiblingTags_thenReturnsSiblingWeight() {
        Tag frontend = createTag("Frontend", TagCategory.SPECIALIZATION);
        Tag react = createTag("React", TagCategory.FRAMEWORK);
        react.setParent(frontend);
        Tag vue = createTag("Vue", TagCategory.FRAMEWORK);
        vue.setParent(frontend);

        Set<Tag> applicantTags = Set.of(react);
        Set<Tag> opportunityTags = Set.of(vue);

        double score = scoringService.computeScorePublic(applicantTags, opportunityTags);

        assertThat(score).isEqualTo(0.7);
    }


    @Test
    @DisplayName("computeScore — навык-дочерний от требования → score = 0.5")
    void computeScore_whenChildOfRequired_thenReturnsHierarchyWeight() {
        Tag frontend = createTag("Frontend", TagCategory.SPECIALIZATION);
        Tag react = createTag("React", TagCategory.FRAMEWORK);
        react.setParent(frontend);

        Set<Tag> applicantTags = Set.of(react);
        Set<Tag> opportunityTags = Set.of(frontend);

        double score = scoringService.computeScorePublic(applicantTags, opportunityTags);

        assertThat(score).isEqualTo(0.5);
    }

    private Tag createTag(String name, TagCategory category) {
        Tag tag = new Tag();
        tag.setId(UUID.randomUUID());
        tag.setName(name);
        tag.setCategory(category);
        tag.setApproved(true);
        tag.setSynonyms(new ArrayList<>());
        return tag;
    }
}