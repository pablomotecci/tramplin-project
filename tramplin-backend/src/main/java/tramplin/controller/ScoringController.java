package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.scoring.ApplicantScoreDto;
import tramplin.dto.scoring.AssignmentResultDto;
import tramplin.dto.scoring.OpportunityScoreDto;
import tramplin.security.UserPrincipal;
import tramplin.service.AssignmentService;
import tramplin.service.ScoringService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/scoring")
@RequiredArgsConstructor
@Tag(name = "Скоринг", description = "Расчёт совместимости соискателей и возможностей")
public class ScoringController {

    private final ScoringService scoringService;
    private final AssignmentService assignmentService;

    @GetMapping("/opportunity/{id}/candidates")
    @PreAuthorize("hasAnyRole('EMPLOYER', 'CURATOR', 'ADMIN')")
    @Operation(summary = "Ранжирование кандидатов", description = "Список соискателей, отсортированный по совместимости с возможностью")
    public ResponseEntity<ApiResponse<List<ApplicantScoreDto>>> getCandidatesForOpportunity(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ApplicantScoreDto> scores = scoringService.calculateScoresForOpportunity(id, principal.getUserId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.ok(scores));
    }

    @GetMapping("/applicant/recommendations")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Рекомендации вакансий", description = "Список возможностей, отсортированный по совместимости с профилем соискателя")
    public ResponseEntity<ApiResponse<List<OpportunityScoreDto>>> getRecommendationsForApplicant(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<OpportunityScoreDto> scores = scoringService.calculateScoresForApplicant(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(scores));
    }

    @GetMapping("/match")
    @PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
    @Operation(summary = "Score совместимости", description = "Конкретный score между соискателем и возможностью")
    public ResponseEntity<ApiResponse<Double>> getMatchScore(
            @RequestParam UUID applicantId,
            @RequestParam UUID opportunityId
    ) {
        double score = scoringService.calculateScore(applicantId, opportunityId);
        return ResponseEntity.ok(ApiResponse.ok(score));
    }

    @GetMapping("/assignment")
    @PreAuthorize("hasAnyRole('CURATOR', 'ADMIN')")
    @Operation(summary = "Оптимальное распределение",
               description = "Венгерский алгоритм: оптимальное назначение соискателей на возможности")
    public ResponseEntity<ApiResponse<AssignmentResultDto>> getOptimalAssignment() {
        AssignmentResultDto result = assignmentService.calculateOptimalAssignment();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}