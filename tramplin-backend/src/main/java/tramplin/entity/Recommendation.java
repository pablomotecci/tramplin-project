package tramplin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recommendations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_recommendation_unique",
                columnNames = {"recommender_id", "recommended_id", "opportunity_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommender_id", nullable = false)
    private ApplicantProfile recommender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_id", nullable = false)
    private ApplicantProfile recommended;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @Column(columnDefinition = "TEXT")
    private String message;
}