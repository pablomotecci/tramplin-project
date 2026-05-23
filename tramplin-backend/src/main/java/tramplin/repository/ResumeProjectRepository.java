package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.ResumeProject;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResumeProjectRepository extends JpaRepository<ResumeProject, UUID> {

    List<ResumeProject> findByApplicantProfileIdOrderByDisplayOrder(UUID applicantProfileId);

    long countByApplicantProfileId(UUID applicantProfileId);
}