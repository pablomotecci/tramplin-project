package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.opportunity.CreateOpportunityRequest;
import tramplin.dto.opportunity.OpportunityMapResponse;
import tramplin.dto.opportunity.OpportunityResponse;
import tramplin.dto.opportunity.UpdateOpportunityRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.WorkFormat;
import tramplin.security.UserPrincipal;
import tramplin.service.OpportunityService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/opportunities")
@RequiredArgsConstructor
@Tag(name = "Вакансии", description = "Создание и просмотр вакансий / стажировок / мероприятий")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Создать вакансию", description = "Доступно только верифицированным работодателям")
    public ResponseEntity<ApiResponse<OpportunityResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOpportunityRequest request
    ) {
        OpportunityResponse response = opportunityService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить вакансию по ID", description = "Доступно всем, включая гостей")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getById(
            @PathVariable UUID id
    ) {
        OpportunityResponse response = opportunityService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Мои вакансии", description = "Список вакансий текущего работодателя с фильтром по статусу")
    public ResponseEntity<ApiResponse<List<OpportunityResponse>>> getMyOpportunities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) OpportunityStatus status
    ) {
        List<OpportunityResponse> response = opportunityService.getMyOpportunities(principal.getUserId(), status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Изменить статус вакансии", description = "Работодатель: ACTIVE, CLOSED, SCHEDULED, DRAFT")
    public ResponseEntity<ApiResponse<OpportunityResponse>> changeStatus(
            @PathVariable UUID id,
            @RequestParam OpportunityStatus status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OpportunityResponse response = opportunityService.changeStatus(id, status, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "Список вакансий", description = "Публичный список с фильтрами и пагинацией")
    public ResponseEntity<ApiResponse<Page<OpportunityResponse>>> getAll(
            @RequestParam(required = false) OpportunityType type,
            @RequestParam(required = false) WorkFormat workFormat,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long salaryMin,
            @RequestParam(required = false) List<UUID> tagIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<OpportunityResponse> response = opportunityService.getAll(type, workFormat, city, salaryMin, tagIds, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/map")
    @Operation(summary = "Маркеры на карте", description = "Вакансии в видимой области карты (bounding box)")
    public ResponseEntity<ApiResponse<List<OpportunityMapResponse>>> getForMap(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng,
            @RequestParam(required = false) List<UUID> tagIds
    ) {
        List<OpportunityMapResponse> response = opportunityService.getForMap(swLat, swLng, neLat, neLng, tagIds);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Обновить вакансию", description = "Частичное обновление — передавай только изменяемые поля")
    public ResponseEntity<ApiResponse<OpportunityResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateOpportunityRequest request
    ) {
        OpportunityResponse response = opportunityService.update(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Удалить вакансию", description = "Только владелец вакансии может её удалить")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        opportunityService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}