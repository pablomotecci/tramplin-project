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

    public static Specification<Opportunity> hasSalaryMin(Long salaryMin) {
        return salaryMin == null
                ? (root, query, cb) -> cb.conjunction()
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("salaryMax"), salaryMin);
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