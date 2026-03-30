package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.TagSynonym;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagSynonymRepository extends JpaRepository<TagSynonym, UUID> {
    Optional<TagSynonym> findBySynonymIgnoreCase(String synonym);
}
