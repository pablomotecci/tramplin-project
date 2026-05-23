package tramplin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resume_projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeProject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_profile_id", nullable = false)
    @ToString.Exclude
    private ApplicantProfile applicantProfile;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String role;

    @Column(name = "start_date", nullable = false, length = 7)
    private String startDate;

    @Column(name = "end_date", length = 7)
    private String endDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "project_url", length = 500)
    private String projectUrl;

    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}