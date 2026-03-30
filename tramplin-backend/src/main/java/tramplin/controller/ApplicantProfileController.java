package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.application.ApplicationResponse;
import tramplin.dto.request.UpdateApplicantRequest;
import tramplin.dto.request.UpdateApplicantTagsRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.ApplicantProfileResponse;
import tramplin.dto.response.ContactResponse;
import tramplin.dto.tag.TagResponse;

import java.util.List;
import java.util.UUID;
import tramplin.security.UserPrincipal;
import tramplin.service.ApplicantProfileService;

@RestController
@RequestMapping("/profile/applicant")
@RequiredArgsConstructor
@Tag(name = "Профиль соискателя", description = "Просмотр и редактирование профиля соискателя")
public class ApplicantProfileController {

    private final ApplicantProfileService applicantProfileService;

    @GetMapping
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Получить свой профиль", description = "Возвращает профиль текущего соискателя")
    public ResponseEntity<ApiResponse<ApplicantProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ApplicantProfileResponse response = applicantProfileService.getProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Обновить свой профиль", description = "Обновляет данные профиля соискателя")
    public ResponseEntity<ApiResponse<ApplicantProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateApplicantRequest request
    ) {
        ApplicantProfileResponse response = applicantProfileService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/tags")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Получить теги соискателя", description = "Навыки и карьерные интересы текущего соискателя")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getTags(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TagResponse> response = applicantProfileService.getTags(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/tags")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Обновить теги соискателя", description = "Полная замена набора навыков/интересов")
    public ResponseEntity<ApiResponse<List<TagResponse>>> updateTags(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateApplicantTagsRequest request
    ) {
        List<TagResponse> response = applicantProfileService.updateTags(principal.getUserId(), request.getTagIds());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Просмотр профиля соискателя",
               description = "Просмотр чужого профиля с учётом настроек приватности")
    public ResponseEntity<ApiResponse<ApplicantProfileResponse>> getPublicProfile(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ApplicantProfileResponse response = applicantProfileService.getPublicProfile(
                userId, principal.getUserId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}/applications")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Отклики соискателя", description = "Просмотр откликов с учётом приватности")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getPublicApplications(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ApplicationResponse> response = applicantProfileService.getPublicApplications(
                userId, principal.getUserId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}/contacts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Контакты соискателя", description = "Просмотр контактов с учётом приватности")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> getPublicContacts(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ContactResponse> response = applicantProfileService.getPublicContacts(
                userId, principal.getUserId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}