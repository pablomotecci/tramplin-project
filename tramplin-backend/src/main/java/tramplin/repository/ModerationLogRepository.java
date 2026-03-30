package tramplin.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.ModerationLog;
import tramplin.entity.enums.TargetType;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, UUID> {

    Page<ModerationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<ModerationLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, UUID targetId);

    Page<ModerationLog> findByCuratorIdOrderByCreatedAtDesc(UUID curatorId, Pageable pageable);
}