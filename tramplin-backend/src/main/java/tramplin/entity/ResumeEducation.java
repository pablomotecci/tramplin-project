package tramplin.entity;

import jakarta.persistence.*;
import lombok.*;
import tramplin.entity.enums.Degree;

@Entity
@Table(name = "resume_education")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeEducation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_profile_id", nullable = false)
    @ToString.Exclude
    private ApplicantProfile applicantProfile;

    @Column(nullable = false, length = 255)
    private String institution;

    @Column(length = 255)
    private String faculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Degree degree;

    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "end_year")
    private Integer endYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;
}