package tramplin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "applicant_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicantProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(length = 255)
    private String university;

    @Column
    private Integer course;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Column(name = "portfolio_url", length = 500)
    private String portfolioUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "skills_summary", columnDefinition = "TEXT")
    private String skillsSummary;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "applicant_tags",
            joinColumns = @JoinColumn(name = "applicant_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}