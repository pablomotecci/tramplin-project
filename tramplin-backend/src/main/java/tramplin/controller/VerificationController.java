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
import tramplin.dto.response.ApiResponse;
import tramplin.dto.verification.CreateVerificationRequestDto;
import tramplin.dto.verification.RejectVerificationDto;
import tramplin.dto.verification.VerificationRequestResponseDto;
import tramplin.security.UserPrincipal;
import tramplin.service.VerificationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
@Tag(name = "Верификация", description = "Подача и проверка заявок на верификацию компаний")
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/request")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Подать заявку на верификацию")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateVerificationRequestDto dto
    ) {
        VerificationRequestResponseDto response = verificationService.createRequest(principal, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Статус моей заявки", description = "Последняя заявка на верификацию")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> getMyRequest(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VerificationRequestResponseDto response = verificationService.getMyRequest(principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/my/history")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "История моих заявок", description = "Все заявки на верификацию")
    public ResponseEntity<ApiResponse<List<VerificationRequestResponseDto>>> getMyRequestHistory(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<VerificationRequestResponseDto> response = verificationService.getMyRequestHistory(principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
    @Operation(summary = "Очередь заявок", description = "Заявки, прошедшие автоматическую проверку и ожидающие решения куратора")
    public ResponseEntity<ApiResponse<Page<VerificationRequestResponseDto>>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        Page<VerificationRequestResponseDto> response = verificationService.getPendingRequests(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
    @Operation(summary = "Одобрить заявку", description = "Компания получает статус VERIFIED")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VerificationRequestResponseDto response = verificationService.approve(id, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
    @Operation(summary = "Отклонить заявку", description = "Куратор указывает причину отклонения")
    public ResponseEntity<ApiResponse<VerificationRequestResponseDto>> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RejectVerificationDto dto
    ) {
        VerificationRequestResponseDto response = verificationService.reject(id, dto, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}