package tramplin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.algorithm.HungarianAlgorithm;
import tramplin.dto.scoring.AssignmentResultDto;
import tramplin.dto.scoring.AssignmentPairDto;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Opportunity;
import tramplin.entity.Tag;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.OpportunityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final ScoringService scoringService;
    private final ApplicantProfileRepository applicantProfileRepository;
    private final OpportunityRepository opportunityRepository;

    @Transactional(readOnly = true)
    public AssignmentResultDto calculateOptimalAssignment() {
        List<ApplicantProfile> applicants = applicantProfileRepository.findAllWithTagsAndUser();
        List<Opportunity> opportunities = opportunityRepository.findAllActiveWithTagsAndEmployer();

        if (applicants.isEmpty() || opportunities.isEmpty()) {
            log.warn("Недостаточно данных для назначения: {} соискателей, {} возможностей",
                    applicants.size(), opportunities.size());
            return AssignmentResultDto.builder()
                    .pairs(List.of())
                    .totalScore(0.0)
                    .applicantCount(applicants.size())
                    .opportunityCount(opportunities.size())
                    .build();
        }

        int rowCount = applicants.size();
        int colCount = opportunities.size();

        double[][] scoreMatrix = new double[rowCount][colCount];
        for (int i = 0; i < rowCount; i++) {
            Set<Tag> applicantTags = applicants.get(i).getTags();
            for (int j = 0; j < colCount; j++) {
                Set<Tag> opportunityTags = opportunities.get(j).getTags();
                scoreMatrix[i][j] = scoringService.computeScorePublic(applicantTags, opportunityTags);
            }
        }

        log.info("Матрица совместимости построена: {}x{}", rowCount, colCount);

        HungarianAlgorithm algorithm = new HungarianAlgorithm(scoreMatrix);
        HungarianAlgorithm.AssignmentResult result = algorithm.solve();

        List<AssignmentPairDto> pairs = new ArrayList<>();
        for (int[] pair : result.getPairs()) {
            int applicantIdx = pair[0];
            int opportunityIdx = pair[1];

            ApplicantProfile applicant = applicants.get(applicantIdx);
            Opportunity opportunity = opportunities.get(opportunityIdx);

            pairs.add(AssignmentPairDto.builder()
                    .applicantId(applicant.getUser().getId())
                    .applicantName(applicant.getUser().getDisplayName())
                    .opportunityId(opportunity.getId())
                    .opportunityTitle(opportunity.getTitle())
                    .companyName(opportunity.getEmployer().getCompanyName())
                    .score(scoreMatrix[applicantIdx][opportunityIdx])
                    .build());
        }

        pairs.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        log.info("Оптимальное назначение: {} пар, суммарный score: {}",
                pairs.size(), String.format("%.2f", result.getTotalScore()));

        return AssignmentResultDto.builder()
                .pairs(pairs)
                .totalScore(result.getTotalScore())
                .applicantCount(rowCount)
                .opportunityCount(colCount)
                .build();
    }
}