package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.Favorite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    List<Favorite> findAllByUserId(UUID userId);

    Optional<Favorite> findByUserIdAndOpportunityId(UUID userId, UUID opportunityId);

    boolean existsByUserIdAndOpportunityId(UUID userId, UUID opportunityId);
}