package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.PlatformStatsResponse;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.Role;
import tramplin.repository.CompanyRepository;
import tramplin.repository.OpportunityRepository;
import tramplin.repository.UserRepository;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Tag(name = "Статистика", description = "Публичная статистика платформы")
public class StatsController {

    private final CompanyRepository companyRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getStats() {
        long companies = companyRepository.count();
        long opportunities = opportunityRepository.countByStatus(OpportunityStatus.ACTIVE);
        long internships = opportunityRepository.countByStatusAndType(
                OpportunityStatus.ACTIVE, OpportunityType.INTERNSHIP);
        long applicants = userRepository.countByRole(Role.APPLICANT);

        PlatformStatsResponse stats = PlatformStatsResponse.builder()
                .companiesCount(companies)
                .opportunitiesCount(opportunities)
                .internshipsCount(internships)
                .applicantsCount(applicants)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
