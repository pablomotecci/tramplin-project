package tramplin.dto.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Снимок совместимости соискателя с вакансией для встраивания в отклик.
 * Виден работодателю — даёт «прозрачность» алгоритма: не только итоговый
 * процент, но и какие именно теги совпали и по какому типу.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationScoreSummary {

    private Integer scorePercent;        // 85, а не 0.85 — единый формат для UI
    private List<MatchedTag> matchedTags;
    private Integer totalRequiredTags;   // сколько всего тегов у вакансии («совпало 4 из 7»)

    public record MatchedTag(
            String tagName,    // "Java"
            String category,   // "LANGUAGE"
            String matchType   // "EXACT" / "SYNONYM" / "SIBLING" / "HIERARCHY"
    ) {}
}
