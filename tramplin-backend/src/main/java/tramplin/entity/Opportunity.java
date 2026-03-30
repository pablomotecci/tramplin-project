package tramplin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.WorkFormat;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "opportunities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Opportunity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_id", nullable = false)
    private Company employer;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OpportunityType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WorkFormat workFormat;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private OpportunityStatus status;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 500)
    private String address;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    private Long salaryMin;

    private Long salaryMax;

    private LocalDateTime publishedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime eventDate;

    @Column(nullable = false, length = 255)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 255)
    private String contactUrl;

    @Column(name = "media_urls", columnDefinition = "TEXT")
    private String mediaUrls;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "opportunity_tags",
            joinColumns = @JoinColumn(name = "opportunity_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}