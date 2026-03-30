package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.tag.SuggestTagRequest;
import tramplin.dto.tag.TagResponse;
import tramplin.service.TagService;

import java.util.UUID;

@RestController
@RequestMapping("/curator/tags")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
@Tag(name = "Управление тегами (Куратор)", description = "Одобрение, отклонение и создание тегов куратором")
public class CuratorTagController {

    private final TagService tagService;

    @GetMapping("/pending")
    @Operation(summary = "Неодобренные теги", description = "Список тегов, ожидающих одобрения")
    public ResponseEntity<ApiResponse<Page<TagResponse>>> getPendingTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TagResponse> response = tagService.getPendingTags(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Одобрить тег", description = "Устанавливает approved=true для тега")
    public ResponseEntity<ApiResponse<TagResponse>> approveTag(@PathVariable UUID id) {
        TagResponse response = tagService.approveTag(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}/reject")
    @Operation(summary = "Отклонить тег", description = "Удаляет неодобренный тег")
    public ResponseEntity<ApiResponse<Void>> rejectTag(@PathVariable UUID id) {
        tagService.rejectTag(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping
    @Operation(summary = "Создать тег", description = "Куратор создаёт тег — он сразу одобрен")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody SuggestTagRequest request) {
        TagResponse response = tagService.createTagByCurator(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}