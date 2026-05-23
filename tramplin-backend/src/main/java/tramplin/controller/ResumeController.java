package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tramplin.dto.resume.CreateEducationRequest;
import tramplin.dto.resume.CreateExperienceRequest;
import tramplin.dto.resume.CreateProjectRequest;
import tramplin.dto.resume.EducationResponse;
import tramplin.dto.resume.ExperienceResponse;
import tramplin.dto.resume.ProjectResponse;
import tramplin.dto.resume.ReorderRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.ResumeEducationService;
import tramplin.service.ResumeExperienceService;
import tramplin.service.ResumeProjectService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profile/applicant/resume")
@RequiredArgsConstructor
@Tag(name = "Резюме соискателя", description = "Опыт, проекты, образование")
public class ResumeController {

    private final ResumeExperienceService experienceService;
    private final ResumeProjectService projectService;
    private final ResumeEducationService educationService;

    // ===== Experiences =====

    @GetMapping("/experiences")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Список опыта работы")
    public ResponseEntity<ApiResponse<List<ExperienceResponse>>> listExperiences(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ExperienceResponse> response = experienceService.list(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/experiences")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Добавить опыт работы")
    public ResponseEntity<ApiResponse<ExperienceResponse>> createExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateExperienceRequest request
    ) {
        ExperienceResponse response = experienceService.create(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/experiences/reorder")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Изменить порядок опыта работы")
    public ResponseEntity<ApiResponse<List<ExperienceResponse>>> reorderExperiences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReorderRequest request
    ) {
        List<ExperienceResponse> response = experienceService.reorder(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/experiences/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Обновить опыт работы")
    public ResponseEntity<ApiResponse<ExperienceResponse>> updateExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateExperienceRequest request
    ) {
        ExperienceResponse response = experienceService.update(principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/experiences/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Удалить опыт работы")
    public ResponseEntity<Void> deleteExperience(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        experienceService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // ===== Projects =====

    @GetMapping("/projects")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Список проектов")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listProjects(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProjectResponse> response = projectService.list(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/projects")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Добавить проект")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectResponse response = projectService.create(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/projects/reorder")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Изменить порядок проектов")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> reorderProjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReorderRequest request
    ) {
        List<ProjectResponse> response = projectService.reorder(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/projects/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Обновить проект")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateProjectRequest request
    ) {
        ProjectResponse response = projectService.update(principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/projects/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Удалить проект")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        projectService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // ===== Education =====

    @GetMapping("/education")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Список образования")
    public ResponseEntity<ApiResponse<List<EducationResponse>>> listEducation(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<EducationResponse> response = educationService.list(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/education")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Добавить образование")
    public ResponseEntity<ApiResponse<EducationResponse>> createEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEducationRequest request
    ) {
        EducationResponse response = educationService.create(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/education/reorder")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Изменить порядок образования")
    public ResponseEntity<ApiResponse<List<EducationResponse>>> reorderEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ReorderRequest request
    ) {
        List<EducationResponse> response = educationService.reorder(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/education/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Обновить образование")
    public ResponseEntity<ApiResponse<EducationResponse>> updateEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateEducationRequest request
    ) {
        EducationResponse response = educationService.update(principal.getUserId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/education/{id}")
    @PreAuthorize("hasRole('APPLICANT')")
    @Operation(summary = "Удалить образование")
    public ResponseEntity<Void> deleteEducation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        educationService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}