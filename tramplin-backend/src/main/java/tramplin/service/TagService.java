package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.tag.SuggestTagRequest;
import tramplin.dto.tag.TagResponse;
import tramplin.dto.tag.TagTreeResponse;
import tramplin.entity.Tag;
import tramplin.entity.TagSynonym;
import tramplin.entity.enums.TagCategory;
import tramplin.exception.ConflictException;
import tramplin.repository.TagRepository;
import tramplin.repository.TagSynonymRepository;
import tramplin.security.UserPrincipal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagSynonymRepository tagSynonymRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> getAllApproved() {
        return tagRepository.findByApprovedTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getByCategory(TagCategory category) {
        return tagRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagResponse> search(String query) {
        return tagRepository.findByNameContainingIgnoreCase(query).stream()
                .filter(Tag::isApproved)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TagTreeResponse> getTree() {
        List<Tag> approved = tagRepository.findByApprovedTrue();

        return approved.stream()
                .filter(tag -> tag.getParent() == null)
                .map(this::mapToTree)
                .toList();
    }

    @Transactional
    public TagResponse suggest(SuggestTagRequest request, UserPrincipal principal) {
        tagRepository.findByNameIgnoreCase(request.getName())
                .ifPresent(existing -> {
                    throw new ConflictException("Тег с именем '" + request.getName() + "' уже существует");
                });

        Tag tag = Tag.builder()
                .name(request.getName())
                .category(request.getCategory())
                .approved(false)
                .build();

        if (request.getParentId() != null) {
            Tag parent = tagRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Родительский тег не найден: " + request.getParentId()));
            tag.setParent(parent);
        }

        Tag saved = tagRepository.save(tag);
        log.info("Предложен новый тег '{}' пользователем {}", saved.getName(), principal.getUserId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TagResponse> getPendingTags(Pageable pageable) {
        return tagRepository.findByApprovedFalseOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public TagResponse approveTag(UUID tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException("Тег не найден: " + tagId));
        tag.setApproved(true);
        Tag saved = tagRepository.save(tag);
        log.info("Тег '{}' одобрен", saved.getName());
        return mapToResponse(saved);
    }

    @Transactional
    public void rejectTag(UUID tagId) {
        if (!tagRepository.existsById(tagId)) {
            throw new EntityNotFoundException("Тег не найден: " + tagId);
        }
        tagRepository.deleteById(tagId);
        log.info("Тег {} отклонён и удалён", tagId);
    }

    @Transactional
    public TagResponse createTagByCurator(SuggestTagRequest dto) {
        tagRepository.findByNameIgnoreCase(dto.getName())
                .ifPresent(existing -> {
                    throw new ConflictException("Тег с именем '" + dto.getName() + "' уже существует");
                });

        Tag tag = Tag.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .approved(true)
                .build();

        if (dto.getParentId() != null) {
            Tag parent = tagRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Родительский тег не найден: " + dto.getParentId()));
            tag.setParent(parent);
        }

        Tag saved = tagRepository.save(tag);
        log.info("Куратор создал тег '{}'", saved.getName());
        return mapToResponse(saved);
    }

    public Optional<Tag> resolveTagByName(String name) {
        Optional<Tag> tag = tagRepository.findByNameIgnoreCase(name);
        if (tag.isPresent()) return tag;
        return tagSynonymRepository.findBySynonymIgnoreCase(name)
                .map(TagSynonym::getTag);
    }

    private TagTreeResponse mapToTree(Tag tag) {
        return TagTreeResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .category(tag.getCategory())
                .children(tag.getChildren().stream()
                        .filter(Tag::isApproved)
                        .map(this::mapToTree)
                        .toList())
                .build();
    }

    private TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .category(tag.getCategory())
                .approved(tag.isApproved())
                .parentId(tag.getParent() != null ? tag.getParent().getId() : null)
                .parentName(tag.getParent() != null ? tag.getParent().getName() : null)
                .build();
    }
}