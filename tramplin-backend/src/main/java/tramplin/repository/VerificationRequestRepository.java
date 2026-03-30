package tramplin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.VerificationRequest;
import tramplin.entity.enums.VerificationRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

    Page<VerificationRequest> findByStatus(VerificationRequestStatus status, Pageable pageable);

    List<VerificationRequest> findByEmployerIdOrderByCreatedAtDesc(UUID employerId);

    Optional<VerificationRequest> findTopByEmployerIdOrderByCreatedAtDesc(UUID employerId);

    boolean existsByEmployerIdAndStatusIn(UUID employerId,
                                          java.util.Collection<VerificationRequestStatus> statuses);
}