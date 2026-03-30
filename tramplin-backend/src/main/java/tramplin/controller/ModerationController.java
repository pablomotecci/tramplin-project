package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.moderation.ModerationActionDto;
import tramplin.dto.moderation.ModerationLogResponseDto;
import tramplin.dto.response.ApiResponse;
import tramplin.entity.enums.TargetType;
import tramplin.security.UserPrincipal;
import tramplin.service.ModerationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
@Tag(name = "Модерация", description = "Скрытие/восстановление вакансий, блокировка/разблокировка пользователей, журнал модерации")
public class ModerationController {

    private final ModerationService moderationService;

    @PutMapping("/opportunities/{id}/hide")
    @Operation(summary = "Скрыть вакансию", description = "Устанавливает статус вакансии HIDDEN")
    public ResponseEntity<ApiResponse<ModerationLogResponseDto>> hideOpportunity(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationActionDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ModerationLogResponseDto response = moderationService.hideOpportunity(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/opportunities/{id}/unhide")
    @Operation(summary = "Восстановить вакансию", description = "Возвращает скрытую вакансию в статус ACTIVE")
    public ResponseEntity<ApiResponse<ModerationLogResponseDto>> unhideOpportunity(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationActionDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ModerationLogResponseDto response = moderationService.unhideOpportunity(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/users/{id}/block")
    @Operation(summary = "Заблокировать пользователя", description = "Блокирует пользователя. Причина обязательна")
    public ResponseEntity<ApiResponse<ModerationLogResponseDto>> blockUser(
            @PathVariable UUID id,
            @RequestBody ModerationActionDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ModerationLogResponseDto response = moderationService.blockUser(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/users/{id}/unblock")
    @Operation(summary = "Разблокировать пользователя", description = "Снимает блокировку с пользователя")
    public ResponseEntity<ApiResponse<ModerationLogResponseDto>> unblockUser(
            @PathVariable UUID id,
            @RequestBody(required = false) ModerationActionDto dto,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ModerationLogResponseDto response = moderationService.unblockUser(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/logs")
    @Operation(summary = "Журнал модерации", description = "Все действия модерации с пагинацией")
    public ResponseEntity<ApiResponse<Page<ModerationLogResponseDto>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ModerationLogResponseDto> response = moderationService.getLogs(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/logs/{targetType}/{targetId}")
    @Operation(summary = "Логи по сущности", description = "История модерации конкретной сущности")
    public ResponseEntity<ApiResponse<List<ModerationLogResponseDto>>> getLogsByTarget(
            @PathVariable TargetType targetType,
            @PathVariable UUID targetId
    ) {
        List<ModerationLogResponseDto> response = moderationService.getLogsByTarget(targetType, targetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}