package tramplin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tramplin.exception.BusinessException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Клиент YandexGPT (Foundation Models API, классический эндпоинт
 * foundationModels/v1/completion). Авторизация по статическому API-ключу
 * сервисного аккаунта: заголовок «Authorization: Api-Key <key>».
 * folder-id передаётся ТОЛЬКО в modelUri, не в заголовке.
 * <p>
 * JSON-режим включается через промпт + ручной парсинг (в отличие от
 * выдуманного поля json_object). Стиль зеркалит {@link YandexGeocoderService}.
 */
@Slf4j
@Service
public class YandexGptService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String folderId;
    private final String model;
    private final int maxTokens;
    private final double temperature;

    public YandexGptService(
            @Value("${app.yandex.gpt.base-url}") String baseUrl,
            @Value("${app.yandex.gpt.api-key}") String apiKey,
            @Value("${app.yandex.gpt.folder-id}") String folderId,
            @Value("${app.yandex.gpt.model}") String model,
            @Value("${app.yandex.gpt.max-tokens}") int maxTokens,
            @Value("${app.yandex.gpt.temperature}") double temperature,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.folderId = folderId;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        log.info("YandexGPT API key configured: {}", apiKey != null && !apiKey.isBlank());
    }

    /**
     * Извлекает навыки из текста резюме. Возвращает список имён тегов так,
     * как их вернула модель (сопоставление с реальным словарём — слой выше).
     */
    public List<String> extractSkills(String resumeText, String catalogPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("AI_UNAVAILABLE",
                    "ИИ-разбор временно недоступен: не настроен ключ YandexGPT");
        }
        try {
            String requestBody = buildRequestBody(resumeText, catalogPrompt);

            String response = webClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Api-Key " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15)) // не виснем на медленной сети во время демо
                    .block();

            return parseSkills(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Запрос к YandexGPT не удался: {}", e.getMessage());
            throw new BusinessException("AI_UNAVAILABLE", "ИИ-сервис недоступен, попробуйте позже");
        }
    }

    private String buildRequestBody(String resumeText, String catalogPrompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("modelUri", "gpt://" + folderId + "/" + model);

        ObjectNode options = root.putObject("completionOptions");
        options.put("stream", false);
        options.put("temperature", temperature);
        options.put("maxTokens", String.valueOf(maxTokens)); // Yandex ждёт строку

        ArrayNode messages = root.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("text", catalogPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("text", resumeText);

        return objectMapper.writeValueAsString(root);
    }

    private List<String> parseSkills(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String raw = root
                    .path("result")
                    .path("alternatives")
                    .get(0)
                    .path("message")
                    .path("text")
                    .asText();

            // Модель иногда оборачивает JSON в ```-фенсы или преамбулу —
            // вырезаем тело объекта между первой { и последней }.
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start < 0 || end < start) {
                log.warn("YandexGPT вернул ответ без JSON-объекта: {}", raw);
                throw new BusinessException("AI_PARSE_FAILED", "Не удалось разобрать ответ ИИ");
            }
            JsonNode parsed = objectMapper.readTree(raw.substring(start, end + 1));

            List<String> skills = new ArrayList<>();
            for (JsonNode tag : parsed.path("tags")) {
                String name = tag.path("name").asText(null);
                if (name != null && !name.isBlank()) {
                    skills.add(name.trim());
                }
            }
            return skills;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Не удалось распарсить ответ YandexGPT: {}", e.getMessage());
            throw new BusinessException("AI_PARSE_FAILED", "Не удалось разобрать ответ ИИ");
        }
    }
}