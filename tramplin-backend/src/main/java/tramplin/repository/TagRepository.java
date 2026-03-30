package tramplin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tramplin.entity.Tag;
import tramplin.entity.enums.TagCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByApprovedTrue();

    List<Tag> findByCategory(TagCategory category);

    Optional<Tag> findByNameIgnoreCase(String name);

    List<Tag> findByNameContainingIgnoreCase(String query);

    List<Tag> findByParentId(UUID parentId);

    List<Tag> findByApprovedFalse();

    Page<Tag> findByApprovedFalseOrderByCreatedAtDesc(Pageable pageable);

}