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
import tramplin.dto.request.UpdateApplicantRequest;
import tramplin.dto.request.UpdateCompanyRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.ApplicantProfileResponse;
import tramplin.dto.response.CompanyProfileResponse;
import tramplin.dto.user.CreateCuratorDto;
import tramplin.dto.user.ResetPasswordResponseDto;
import tramplin.dto.user.UserManagementResponseDto;
import tramplin.entity.enums.AccountStatus;
import tramplin.entity.enums.Role;
import tramplin.service.ApplicantProfileService;
import tramplin.service.CompanyService;
import tramplin.service.UserManagementService;

import java.util.UUID;


@RestController
@RequestMapping("/curator/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
@Tag(name = "Управление пользователями (Куратор)", description = "Просмотр, редактирование профилей, управление статусами")
public class CuratorController {

    private final UserManagementService userManagementService;
    private final ApplicantProfileService applicantProfileService;
    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "Список пользователей", description = "Все пользователи с опциональной фильтрацией по роли или статусу")
    public ResponseEntity<ApiResponse<Page<UserManagementResponseDto>>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserManagementResponseDto> response;
        if (role != null && status != null) {
            response = userManagementService.getUsersByRoleAndStatus(role, status, PageRequest.of(page, size));
        } else if (role != null) {
            response = userManagementService.getUsersByRole(role, PageRequest.of(page, size));
        } else if (status != null) {
            response = userManagementService.getUsersByStatus(status, PageRequest.of(page, size));
        } else {
            response = userManagementService.getAllUsers(PageRequest.of(page, size));
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "Изменить статус пользователя", description = "Блокировка/разблокировка аккаунта")
    public ResponseEntity<ApiResponse<UserManagementResponseDto>> changeStatus(
            @PathVariable UUID userId,
            @RequestParam AccountStatus status
    ) {
        UserManagementResponseDto response = userManagementService.changeStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Сбросить пароль пользователя", description = "Генерирует временный пароль и возвращает его куратору")
    public ResponseEntity<ApiResponse<ResetPasswordResponseDto>> resetPassword(
            @PathVariable UUID userId
    ) {
        ResetPasswordResponseDto response = userManagementService.resetPassword(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{userId}/applicant-profile")
    @Operation(summary = "Редактировать профиль соискателя", description = "Куратор вносит изменения в профиль соискателя")
    public ResponseEntity<ApiResponse<ApplicantProfileResponse>> editApplicantProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateApplicantRequest request
    ) {
        ApplicantProfileResponse response = applicantProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{userId}/company-profile")
    @Operation(summary = "Редактировать профиль компании", description = "Куратор вносит изменения в профиль компании")
    public ResponseEntity<ApiResponse<CompanyProfileResponse>> editCompanyProfile(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateCompanyRequest request
    ) {
        CompanyProfileResponse response = companyService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/curators")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать куратора", description = "Создание нового куратора. Доступно только администратору")
    public ResponseEntity<ApiResponse<UserManagementResponseDto>> createCurator(
            @Valid @RequestBody CreateCuratorDto dto
    ) {
        UserManagementResponseDto response = userManagementService.createCurator(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}