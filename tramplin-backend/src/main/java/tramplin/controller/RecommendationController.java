package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.recommendation.CreateRecommendationRequest;
import tramplin.dto.recommendation.RecommendationResponse;
import tramplin.dto.response.ApiResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.RecommendationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "Рекомендации", description = "Рекомендации контактов на вакансии (LinkedIn referrals)")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Рекомендовать контакт", description = "Рекомендовать своего контакта на активную вакансию")
    public ResponseEntity<ApiResponse<RecommendationResponse>> create(
            @Valid @RequestBody CreateRecommendationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RecommendationResponse response = recommendationService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Мои рекомендации", description = "Список отправленных мной рекомендаций")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getMy(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RecommendationResponse> response = recommendationService.getMy(principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/for-me")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Рекомендации мне", description = "Список рекомендаций в мою пользу")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getForMe(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RecommendationResponse> response = recommendationService.getForMe(principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/opportunity/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Рекомендации по вакансии", description = "Работодатель видит рекомендации на свою вакансию")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getByOpportunity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RecommendationResponse> response = recommendationService.getByOpportunity(id, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}