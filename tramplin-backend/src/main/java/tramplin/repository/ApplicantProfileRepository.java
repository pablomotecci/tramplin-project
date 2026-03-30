package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tramplin.entity.ApplicantProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicantProfileRepository extends JpaRepository<ApplicantProfile, UUID> {

    Optional<ApplicantProfile> findByUserId(UUID userId);

    @Query("SELECT ap FROM ApplicantProfile ap " +
           "LEFT JOIN FETCH ap.tags t " +
           "LEFT JOIN FETCH t.parent " +
           "LEFT JOIN FETCH t.synonyms " +
           "WHERE ap.user.id = :userId")
    Optional<ApplicantProfile> findByUserIdWithTags(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT ap FROM ApplicantProfile ap " +
           "LEFT JOIN FETCH ap.tags t " +
           "LEFT JOIN FETCH t.parent " +
           "LEFT JOIN FETCH t.synonyms " +
           "LEFT JOIN FETCH ap.user")
    List<ApplicantProfile> findAllWithTagsAndUser();
}

