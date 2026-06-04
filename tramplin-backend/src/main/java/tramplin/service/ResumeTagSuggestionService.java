package tramplin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.resume.SuggestedTagDto;
import tramplin.entity.Tag;
import tramplin.repository.TagRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Оркестратор ИИ-тегирования: каталог одобренных тегов → промпт → YandexGPT →
 * сопоставление извлечённых навыков с реальным словарём через
 * {@link TagService#resolveTagByName}.
 * <p>
 * Ничего не сохраняет — возвращает только предложения. Запись тегов в профиль —
 * отдельный осознанный шаг человека через существующий PUT
 * /profile/applicant/tags.
 * Синонимы в промпт не уходят: модель оперирует каноническими именами, а
 * синонимы
 * отрабатывают на обратном пути в resolveTagByName. Любой выдуманный навык,
 * которого
 * нет в словаре, отсекается на этом шаге и в систему не попадает.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTagSuggestionService {

    private final TagRepository tagRepository;
    private final TagService tagService;
    private final YandexGptService yandexGptService;

    @Transactional(readOnly = true)
    public List<SuggestedTagDto> suggestTags(UUID userId, String resumeText) {
        // Audit-лог обращений к ИИ: кто и когда запускал разбор резюме.
        log.info("Соискатель {} запросил ИИ-разбор резюме", userId);

        List<Tag> catalog = tagRepository.findByApprovedTrue();
        if (catalog.isEmpty()) {
            return List.of();
        }

        String catalogPrompt = buildCatalogPrompt(catalog);
        List<String> skills = yandexGptService.extractSkills(resumeText, catalogPrompt);

        // LinkedHashMap: дедуп по tagId с сохранением порядка появления навыков.
        Map<UUID, SuggestedTagDto> byId = new LinkedHashMap<>();
        for (String skill : skills) {
            // Страховка: модель иногда дописывает категорию в скобках, например
            // "Java (LANGUAGE)". Отрезаем хвост "(...)" перед сопоставлением со словарём.
            String cleaned = skill.replaceAll("\\s*\\(.*\\)\\s*$", "").trim();
            tagService.resolveTagByName(cleaned)
                    .ifPresent(tag -> byId.putIfAbsent(tag.getId(), SuggestedTagDto.builder()
                            .tagId(tag.getId())
                            .name(tag.getName())
                            .category(tag.getCategory())
                            .build()));
        }

        log.info("ИИ предложил {} навыков, сопоставлено со словарём: {}", skills.size(), byId.size());
        return new ArrayList<>(byId.values());
    }

    private String buildCatalogPrompt(List<Tag> catalog) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ты извлекаешь профессиональные навыки из текста резюме. ")
                .append("Сопоставляй каждый навык ТОЛЬКО с тегами из каталога ниже. ")
                .append("В поле \"name\" укажи ТОЛЬКО имя тега, БЕЗ категории и БЕЗ скобок. ")
                .append("Не выдумывай теги, которых нет в каталоге. ")
                .append("Верни строго JSON вида {\"tags\":[{\"name\":\"Java\"},{\"name\":\"Docker\"}]} ")
                .append("без пояснений и без markdown.\n\n")
                .append("Каталог тегов:\n");
        for (Tag tag : catalog) {
            sb.append("- ").append(tag.getName())
                    .append("  [категория: ").append(tag.getCategory()).append("]\n");
        }
        return sb.toString();
    }
}
