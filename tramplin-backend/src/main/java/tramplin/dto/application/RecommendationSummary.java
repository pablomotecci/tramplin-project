package tramplin.dto.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Карточка-снимок рекомендации, встраиваемая в отклик (ApplicationResponse).
 * Виден только работодателю, открывающему отклик. Не путать с публичным
 * RecommendationResponse под контроллером /recommendations/by-opportunity/*.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSummary {

    private UUID recommenderUserId;
    private String recommenderName;
    private String recommenderAvatarUrl;
    private String message;
    private LocalDateTime createdAt;
}
