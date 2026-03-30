package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.PrivacySettings;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrivacySettingsRepository extends JpaRepository<PrivacySettings, UUID> {

    Optional<PrivacySettings> findByApplicantId(UUID applicantId);

    Optional<PrivacySettings> findByApplicantUserId(UUID userId);
}