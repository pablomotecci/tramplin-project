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
import tramplin.dto.application.ApplicationResponse;
import tramplin.dto.application.CreateApplicationRequest;
import tramplin.dto.application.UpdateApplicationStatusRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.entity.enums.ApplicationStatus;
import tramplin.security.UserPrincipal;
import tramplin.service.ApplicationService;

import java.util.UUID;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Tag(name = "Отклики", description = "Подача откликов соискателями и управление статусами работодателями")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Откликнуться на вакансию", description = "Доступно только соискателям")
    public ResponseEntity<ApiResponse<ApplicationResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        ApplicationResponse response = applicationService.createApplication(principal, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Мои отклики", description = "Список откликов текущего соискателя")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ApplicationResponse> response = applicationService.getMyApplications(principal, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Входящие отклики", description = "Отклики на вакансии текущего работодателя")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> getIncomingApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ApplicationResponse> response = applicationService.getIncomingApplications(principal, pageable, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('APPLICANT', 'EMPLOYER')")
    @Operation(summary = "Детали отклика", description = "Доступно автору отклика и владельцу вакансии")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        ApplicationResponse response = applicationService.getApplicationById(principal, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Изменить статус отклика", description = "Только владелец вакансии может менять статус")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApplicationStatusRequest request
    ) {
        ApplicationResponse response = applicationService.updateStatus(principal, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
