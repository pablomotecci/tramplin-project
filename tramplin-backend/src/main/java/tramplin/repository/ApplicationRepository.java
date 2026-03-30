package tramplin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tramplin.entity.Application;
import tramplin.entity.enums.ApplicationStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Page<Application> findByApplicantId(UUID applicantId, Pageable pageable);

    Page<Application> findByOpportunityEmployerId(UUID employerId, Pageable pageable);

    List<Application> findByApplicantUserId(UUID userId);

    @Query("SELECT a FROM Application a " +
           "LEFT JOIN FETCH a.opportunity o " +
           "LEFT JOIN FETCH o.employer " +
           "LEFT JOIN FETCH a.applicant ap " +
           "LEFT JOIN FETCH ap.user " +
           "WHERE ap.user.id = :userId")
    List<Application> findByApplicantUserIdWithDetails(@Param("userId") UUID userId);

    boolean existsByApplicantIdAndOpportunityId(UUID applicantId, UUID opportunityId);

    Page<Application> findByOpportunityEmployerIdAndStatus(UUID employerId, ApplicationStatus status, Pageable pageable);
}
