package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.request.UpdateCompanyRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.CompanyProfileResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.CompanyService;

@RestController
@RequestMapping("/profile/employer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
@Tag(name = "Профиль работодателя", description = "Просмотр и редактирование профиля компании")
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    @Operation(summary = "Получить профиль компании", description = "Возвращает профиль компании текущего работодателя")
    public ResponseEntity<ApiResponse<CompanyProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        CompanyProfileResponse response = companyService.getProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    @Operation(summary = "Обновить профиль компании", description = "Обновляет данные профиля компании")
    public ResponseEntity<ApiResponse<CompanyProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCompanyRequest request
    ) {
        CompanyProfileResponse response = companyService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}