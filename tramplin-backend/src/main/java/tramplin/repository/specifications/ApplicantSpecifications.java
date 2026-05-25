package tramplin.repository.specifications;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.ContactRequest;
import tramplin.entity.PrivacySettings;
import tramplin.entity.Tag;
import tramplin.entity.enums.ContactRequestStatus;
import tramplin.entity.enums.Visibility;

import java.util.Set;
import java.util.UUID;

public final class ApplicantSpecifications {

    private ApplicantSpecifications() {
    }

    public static Specification<ApplicantProfile> hasQueryInNameOrUniversity(String query) {
        if (query == null || query.isBlank()) {
            return (root, q, cb) -> cb.conjunction();
        }
        String pattern = "%" + query.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(cb.coalesce(root.<String>get("middleName"), "")), pattern),
                cb.like(cb.lower(cb.coalesce(root.<String>get("university"), "")), pattern)
        );
    }

    /**
     * «AND по тегам»: соискатель должен иметь ВСЕ указанные теги.
     * Реализовано как один подзапрос с GROUP BY + HAVING COUNT(DISTINCT), а не N EXISTS-подзапросов,
     * чтобы избежать N+1 на стороне БД. GROUP BY вынесен в SUBQUERY — снаружи остаётся обычный IN,
     * поэтому пагинация Spring Data JPA работает корректно (count-запрос не ломается).
     */
    public static Specification<ApplicantProfile> hasAllTags(Set<UUID> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return (root, q, cb) -> cb.conjunction();
        }
        return (root, q, cb) -> {
            Subquery<UUID> sub = q.subquery(UUID.class);
            Root<ApplicantProfile> apSub = sub.from(ApplicantProfile.class);
            Join<ApplicantProfile, Tag> tagJoin = apSub.join("tags");
            sub.select(apSub.get("id"))
                    .where(tagJoin.get("id").in(tagIds))
                    .groupBy(apSub.get("id"))
                    .having(cb.equal(cb.countDistinct(tagJoin.get("id")), (long) tagIds.size()));
            return root.get("id").in(sub);
        };
    }

    public static Specification<ApplicantProfile> hasUniversity(String university) {
        if (university == null || university.isBlank()) {
            return (root, q, cb) -> cb.conjunction();
        }
        String pattern = "%" + university.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("university")), pattern);
    }

    public static Specification<ApplicantProfile> graduationYearBetween(Integer min, Integer max) {
        return (root, q, cb) -> {
            if (min == null && max == null) {
                return cb.conjunction();
            }
            Path<Integer> year = root.get("graduationYear");
            if (min != null && max != null) {
                return cb.between(year, min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(year, min);
            }
            return cb.lessThanOrEqualTo(year, max);
        };
    }

    /**
     * SQL-фильтр приватности для поиска: соискатель попадает в выдачу, только если viewer вправе его видеть.
     * Логика (повторяет {@code PrivacyService.canView}, но в WHERE — для корректной пагинации):
     * <ul>
     *   <li>CURATOR / ADMIN — без ограничений;</li>
     *   <li>свой профиль — всегда;</li>
     *   <li>нет {@code privacy_settings} — видно (backward compat для записей до фичи);</li>
     *   <li>{@code profileVisibility = ALL} — видно;</li>
     *   <li>{@code profileVisibility = EMPLOYERS_ONLY} — видно, если viewerRole = EMPLOYER;</li>
     *   <li>{@code profileVisibility = CONTACTS_ONLY} — видно, если есть ACCEPTED-связь в contact_requests;</li>
     *   <li>{@code profileVisibility = NOBODY} — скрыто.</li>
     * </ul>
     */
    public static Specification<ApplicantProfile> visibleTo(UUID viewerId, String viewerRole) {
        return (root, q, cb) -> {
            if ("CURATOR".equals(viewerRole) || "ADMIN".equals(viewerRole)) {
                return cb.conjunction();
            }

            Predicate self = cb.equal(root.get("user").get("id"), viewerId);

            Subquery<Long> hasPsSub = q.subquery(Long.class);
            Root<PrivacySettings> noPsRoot = hasPsSub.from(PrivacySettings.class);
            hasPsSub.select(cb.literal(1L))
                    .where(cb.equal(noPsRoot.get("applicant").get("id"), root.get("id")));
            Predicate noSettings = cb.not(cb.exists(hasPsSub));

            Subquery<Long> allowedSub = q.subquery(Long.class);
            Root<PrivacySettings> ps = allowedSub.from(PrivacySettings.class);
            Path<Visibility> vis = ps.get("profileVisibility");

            Predicate visAll = cb.equal(vis, Visibility.ALL);
            Predicate visEmployers = "EMPLOYER".equals(viewerRole)
                    ? cb.equal(vis, Visibility.EMPLOYERS_ONLY)
                    : cb.disjunction();

            Subquery<Long> contactSub = q.subquery(Long.class);
            Root<ContactRequest> cr = contactSub.from(ContactRequest.class);
            contactSub.select(cb.literal(1L))
                    .where(cb.and(
                            cb.equal(cr.get("status"), ContactRequestStatus.ACCEPTED),
                            cb.or(
                                    cb.and(
                                            cb.equal(cr.get("sender").get("id"), root.get("user").get("id")),
                                            cb.equal(cr.get("receiver").get("id"), viewerId)
                                    ),
                                    cb.and(
                                            cb.equal(cr.get("sender").get("id"), viewerId),
                                            cb.equal(cr.get("receiver").get("id"), root.get("user").get("id"))
                                    )
                            )
                    ));
            Predicate visContacts = cb.and(
                    cb.equal(vis, Visibility.CONTACTS_ONLY),
                    cb.exists(contactSub)
            );

            allowedSub.select(cb.literal(1L))
                    .where(cb.and(
                            cb.equal(ps.get("applicant").get("id"), root.get("id")),
                            cb.or(visAll, visEmployers, visContacts)
                    ));

            return cb.or(self, noSettings, cb.exists(allowedSub));
        };
    }
}