package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tramplin.entity.Opportunity;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.entity.enums.OpportunityType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID>, JpaSpecificationExecutor<Opportunity> {

    @Query("SELECT o FROM Opportunity o " +
           "LEFT JOIN FETCH o.tags t " +
           "LEFT JOIN FETCH t.parent " +
           "LEFT JOIN FETCH t.synonyms " +
           "LEFT JOIN FETCH o.employer " +
           "WHERE o.id = :id")
    Optional<Opportunity> findByIdWithTagsAndEmployer(@Param("id") UUID id);

    @Query("SELECT DISTINCT o FROM Opportunity o " +
           "LEFT JOIN FETCH o.tags t " +
           "LEFT JOIN FETCH t.parent " +
           "LEFT JOIN FETCH t.synonyms " +
           "LEFT JOIN FETCH o.employer " +
           "WHERE o.status = 'ACTIVE'")
    List<Opportunity> findAllActiveWithTagsAndEmployer();

    List<Opportunity> findByStatus(OpportunityStatus status);

    List<Opportunity> findByEmployerId(UUID employerId);

    List<Opportunity> findByEmployerIdAndStatus(UUID employerId, OpportunityStatus status);

    long countByStatus(OpportunityStatus status);

    long countByStatusAndType(OpportunityStatus status, OpportunityType type);

    @Query("SELECT o FROM Opportunity o WHERE o.status = 'ACTIVE' " +
            "AND ST_Within(o.location, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326)) = true")
    List<Opportunity> findInBoundingBox(
            @Param("swLng") double swLng,
            @Param("swLat") double swLat,
            @Param("neLng") double neLng,
            @Param("neLat") double neLat
    );

    @Query("SELECT DISTINCT o FROM Opportunity o JOIN o.tags t WHERE o.status = 'ACTIVE' " +
            "AND ST_Within(o.location, ST_MakeEnvelope(:swLng, :swLat, :neLng, :neLat, 4326)) = true " +
            "AND t.id IN :tagIds")
    List<Opportunity> findInBoundingBoxWithTags(
            @Param("swLng") double swLng,
            @Param("swLat") double swLat,
            @Param("neLng") double neLng,
            @Param("neLat") double neLat,
            @Param("tagIds") List<UUID> tagIds
    );
}
