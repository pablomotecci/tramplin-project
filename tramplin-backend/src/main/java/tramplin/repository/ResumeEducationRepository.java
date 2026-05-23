package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.ResumeEducation;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeEducationRepository extends JpaRepository<ResumeEducation, UUID> {

    List<ResumeEducation> findByApplicantProfileIdOrderByDisplayOrder(UUID applicantProfileId);

    long countByApplicantProfileId(UUID applicantProfileId);
}