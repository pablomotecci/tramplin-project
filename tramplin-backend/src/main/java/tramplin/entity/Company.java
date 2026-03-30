package tramplin.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import tramplin.entity.enums.VerificationStatus;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    private User user;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String industry;

    @Column(length = 12, unique = true)
    private String inn;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(length = 100)
    private String city;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "office_photos", columnDefinition = "TEXT")
    private String officePhotos;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
}