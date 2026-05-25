package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tramplin.dto.request.ApplicantSearchRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.ApplicantSearchResultResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.ApplicantSearchService;

@RestController
@RequestMapping("/applicants")
@RequiredArgsConstructor
@Tag(name = "Поиск соискателей", description = "Поиск по фильтрам с учётом приватности")
public class ApplicantSearchController {

    private final ApplicantSearchService applicantSearchService;

    @GetMapping("/search")
    @PreAuthorize("hasRole('APPLICANT') or hasRole('EMPLOYER')")
    @Operation(summary = "Поиск соискателей по фильтрам",
            description = "Учитывает profileVisibility соискателей: NOBODY, скрытые EMPLOYERS_ONLY/CONTACTS_ONLY не попадают в выдачу")
    public ResponseEntity<ApiResponse<Page<ApplicantSearchResultResponse>>> search(
            @ModelAttribute ApplicantSearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<ApplicantSearchResultResponse> result = applicantSearchService.search(
                request, principal.getUserId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}