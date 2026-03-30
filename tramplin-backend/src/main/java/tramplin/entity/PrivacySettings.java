package tramplin.entity;

import jakarta.persistence.*;
import lombok.*;
import tramplin.entity.enums.Visibility;

@Entity
@Table(name = "privacy_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacySettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false, unique = true)
    @ToString.Exclude
    private ApplicantProfile applicant;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 20)
    @Builder.Default
    private Visibility profileVisibility = Visibility.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "resume_visibility", nullable = false, length = 20)
    @Builder.Default
    private Visibility resumeVisibility = Visibility.CONTACTS_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "applications_visibility", nullable = false, length = 20)
    @Builder.Default
    private Visibility applicationsVisibility = Visibility.NOBODY;

    @Enumerated(EnumType.STRING)
    @Column(name = "contacts_visibility", nullable = false, length = 20)
    @Builder.Default
    private Visibility contactsVisibility = Visibility.CONTACTS_ONLY;
}