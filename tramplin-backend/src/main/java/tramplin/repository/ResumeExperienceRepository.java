package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.ResumeExperience;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeExperienceRepository extends JpaRepository<ResumeExperience, UUID> {

    List<ResumeExperience> findByApplicantProfileIdOrderByDisplayOrder(UUID applicantProfileId);

    long countByApplicantProfileId(UUID applicantProfileId);
}