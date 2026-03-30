package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.CompanyProfileResponse;
import tramplin.service.CompanyService;
import java.util.UUID;

@RestController
@RequestMapping("/employers")
@RequiredArgsConstructor
@Tag(name = "Публичный профиль компании", description = "Просмотр профиля компании без авторизации")
public class EmployerPublicController {

    private final CompanyService companyService;

    @GetMapping("/{companyId}/public")
    @Operation(summary = "Публичный профиль компании",
               description = "Доступен всем, включая неавторизованных пользователей")
    public ResponseEntity<ApiResponse<CompanyProfileResponse>> getPublicProfile(
            @PathVariable UUID companyId
    ) {
        CompanyProfileResponse response = companyService.getPublicProfile(companyId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}