package tramplin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resume_experiences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeExperience extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_profile_id", nullable = false)
    @ToString.Exclude
    private ApplicantProfile applicantProfile;

    @Column(nullable = false, length = 255)
    private String organization;

    @Column(nullable = false, length = 255)
    private String position;

    /** Формат YYYY-MM */
    @Column(name = "start_date", nullable = false, length = 7)
    private String startDate;

    /** Формат YYYY-MM. NULL = "по настоящее время" */
    @Column(name = "end_date", length = 7)
    private String endDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}