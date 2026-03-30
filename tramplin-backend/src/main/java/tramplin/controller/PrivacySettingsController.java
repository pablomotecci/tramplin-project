package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.privacy.PrivacySettingsDto;
import tramplin.dto.response.ApiResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.PrivacySettingsService;

@RestController
@RequestMapping("/privacy-settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('APPLICANT')")
@Tag(name = "Настройки приватности", description = "Гранулярное управление видимостью разделов профиля соискателя")
public class PrivacySettingsController {

    private final PrivacySettingsService privacySettingsService;

    @GetMapping
    @Operation(summary = "Получить настройки приватности", description = "Текущие настройки видимости разделов профиля")
    public ResponseEntity<ApiResponse<PrivacySettingsDto>> getSettings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        PrivacySettingsDto response = privacySettingsService.getSettings(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    @Operation(summary = "Обновить настройки приватности", description = "Частичное обновление — отправляйте только те поля, которые хотите изменить")
    public ResponseEntity<ApiResponse<PrivacySettingsDto>> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody PrivacySettingsDto dto
    ) {
        PrivacySettingsDto response = privacySettingsService.updateSettings(principal.getUserId(), dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}