package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.resume.CreateProjectRequest;
import tramplin.dto.resume.ProjectResponse;
import tramplin.dto.resume.ReorderRequest;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.ResumeProject;
import tramplin.exception.BusinessException;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.ResumeProjectRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeProjectService {

    private static final int MAX_ITEMS = 30;

    private final ResumeProjectRepository projectRepository;
    private final ApplicantProfileRepository applicantProfileRepository;

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID userId) {
        ApplicantProfile profile = getProfile(userId);
        return projectRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse create(UUID userId, CreateProjectRequest request) {
        ApplicantProfile profile = getProfile(userId);
        validateDates(request.getStartDate(), request.getEndDate());
        validateUrl(request.getProjectUrl());
        validateUrl(request.getRepositoryUrl());

        long count = projectRepository.countByApplicantProfileId(profile.getId());
        if (count >= MAX_ITEMS) {
            throw new BusinessException("LIMIT_EXCEEDED",
                    "Превышен лимит проектов: до " + MAX_ITEMS);
        }

        ResumeProject project = ResumeProject.builder()
                .applicantProfile(profile)
                .title(request.getTitle())
                .role(request.getRole())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .projectUrl(request.getProjectUrl())
                .repositoryUrl(request.getRepositoryUrl())
                .displayOrder((int) count)
                .build();

        ResumeProject saved = projectRepository.save(project);
        log.info("Создан проект: id={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse update(UUID userId, UUID projectId, CreateProjectRequest request) {
        ResumeProject project = getOwnedProject(userId, projectId);
        validateDates(request.getStartDate(), request.getEndDate());
        validateUrl(request.getProjectUrl());
        validateUrl(request.getRepositoryUrl());

        project.setTitle(request.getTitle());
        project.setRole(request.getRole());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        project.setDescription(request.getDescription());
        project.setProjectUrl(request.getProjectUrl());
        project.setRepositoryUrl(request.getRepositoryUrl());

        ResumeProject saved = projectRepository.save(project);
        log.info("Обновлён проект: id={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID projectId) {
        ResumeProject project = getOwnedProject(userId, projectId);
        projectRepository.delete(project);
        log.info("Удалён проект: id={}, userId={}", projectId, userId);
    }

    @Transactional
    public List<ProjectResponse> reorder(UUID userId, ReorderRequest request) {
        ApplicantProfile profile = getProfile(userId);
        List<ResumeProject> all = projectRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId());

        Map<UUID, ResumeProject> byId = all.stream()
                .collect(Collectors.toMap(ResumeProject::getId, p -> p));

        for (ReorderRequest.ReorderItem item : request.getItems()) {
            ResumeProject p = byId.get(item.getId());
            if (p == null) {
                throw new AccessDeniedException("Проект не принадлежит соискателю");
            }
            p.setDisplayOrder(item.getDisplayOrder());
        }

        projectRepository.saveAll(all);
        log.info("Изменён порядок проектов: userId={}", userId);

        return projectRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private ApplicantProfile getProfile(UUID userId) {
        return applicantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден: " + userId));
    }

    private ResumeProject getOwnedProject(UUID userId, UUID projectId) {
        ResumeProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Проект не найден: " + projectId));

        if (!project.getApplicantProfile().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Проект не принадлежит соискателю");
        }
        return project;
    }

    private void validateDates(String startDate, String endDate) {
        if (endDate != null && endDate.compareTo(startDate) < 0) {
            throw new BusinessException("INVALID_DATE_RANGE",
                    "Дата окончания не может быть раньше даты начала");
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BusinessException("INVALID_URL",
                    "URL должен начинаться с http:// или https://");
        }
    }

    private ProjectResponse toResponse(ResumeProject p) {
        return ProjectResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .role(p.getRole())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .description(p.getDescription())
                .projectUrl(p.getProjectUrl())
                .repositoryUrl(p.getRepositoryUrl())
                .displayOrder(p.getDisplayOrder())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}