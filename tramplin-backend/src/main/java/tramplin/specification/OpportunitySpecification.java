package tramplin.specification;

import org.springframework.data.jpa.domain.Specification;
import tramplin.entity.Opportunity;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.WorkFormat;
import tramplin.entity.enums.OpportunityStatus;
import jakarta.persistence.criteria.JoinType;

import java.util.List;
import java.util.UUID;

public class OpportunitySpecification {

    public static Specification<Opportunity> hasStatus(OpportunityStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Opportunity> hasType(OpportunityType type) {
        return type == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Opportunity> hasWorkFormat(WorkFormat workFormat) {
        return workFormat == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> cb.equal(root.get("workFormat"), workFormat);
    }

    public static Specification<Opportunity> hasCity(String city) {
        return city == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> cb.equal(root.get("city"), city);
    }

    /**
     * Вхождение искомой суммы S в вилку вакансии [salaryMin, salaryMax] включительно.
     * Открытые вилки трактуются как полуоткрытые интервалы: null-граница = ±∞.
     *   (salaryMin IS NULL OR salaryMin <= S) AND (salaryMax IS NULL OR salaryMax >= S)
     * Так вакансия «от 100k» (salaryMax не задан) корректно находится при поиске 250k,
     * потому что 250k ∈ [100k, +∞). Вакансия с обеими null-границами подходит под любой S
     * (зарплата не указана → не прячем). Фильтрация на уровне SQL, совместима с пагинацией.
     */
    public static Specification<Opportunity> salaryContains(Long salary) {
        return salary == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> cb.and(
                        cb.or(
                                cb.isNull(root.get("salaryMin")),
                                cb.lessThanOrEqualTo(root.get("salaryMin"), salary)),
                        cb.or(
                                cb.isNull(root.get("salaryMax")),
                                cb.greaterThanOrEqualTo(root.get("salaryMax"), salary)));
    }

    public static Specification<Opportunity> hasTags(List<UUID> tagIds) {
        return tagIds == null || tagIds.isEmpty()
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> {
                    query.distinct(true);
                    var tagsJoin = root.join("tags");
                    return tagsJoin.get("id").in(tagIds);
                };
    }

    public static Specification<Opportunity> searchByText(String search) {
        return (search == null || search.isBlank())
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("employer").get("companyName")), pattern)
            );
        };
    }
}