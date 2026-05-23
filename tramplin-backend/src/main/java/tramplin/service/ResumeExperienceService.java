package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.resume.CreateExperienceRequest;
import tramplin.dto.resume.ExperienceResponse;
import tramplin.dto.resume.ReorderRequest;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.ResumeExperience;
import tramplin.exception.BusinessException;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.ResumeExperienceRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExperienceService {

    private static final int MAX_ITEMS = 20;

    private final ResumeExperienceRepository experienceRepository;
    private final ApplicantProfileRepository applicantProfileRepository;

    @Transactional(readOnly = true)
    public List<ExperienceResponse> list(UUID userId) {
        ApplicantProfile profile = getProfile(userId);
        return experienceRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExperienceResponse create(UUID userId, CreateExperienceRequest request) {
        ApplicantProfile profile = getProfile(userId);

        validateDates(request.getStartDate(), request.getEndDate());

        long count = experienceRepository.countByApplicantProfileId(profile.getId());
        if (count >= MAX_ITEMS) {
            throw new BusinessException("LIMIT_EXCEEDED",
                    "Превышен лимит записей опыта: до " + MAX_ITEMS);
        }

        ResumeExperience experience = ResumeExperience.builder()
                .applicantProfile(profile)
                .organization(request.getOrganization())
                .position(request.getPosition())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .description(request.getDescription())
                .displayOrder((int) count)  // в конец списка
                .build();

        ResumeExperience saved = experienceRepository.save(experience);
        log.info("Создана запись опыта: id={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public ExperienceResponse update(UUID userId, UUID experienceId, CreateExperienceRequest request) {
        ResumeExperience experience = getOwnedExperience(userId, experienceId);

        validateDates(request.getStartDate(), request.getEndDate());

        experience.setOrganization(request.getOrganization());
        experience.setPosition(request.getPosition());
        experience.setStartDate(request.getStartDate());
        experience.setEndDate(request.getEndDate());
        experience.setDescription(request.getDescription());

        ResumeExperience saved = experienceRepository.save(experience);
        log.info("Обновлена запись опыта: id={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID experienceId) {
        ResumeExperience experience = getOwnedExperience(userId, experienceId);
        experienceRepository.delete(experience);
        log.info("Удалена запись опыта: id={}, userId={}", experienceId, userId);
    }

    @Transactional
    public List<ExperienceResponse> reorder(UUID userId, ReorderRequest request) {
        ApplicantProfile profile = getProfile(userId);
        List<ResumeExperience> all = experienceRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId());

        Map<UUID, ResumeExperience> byId = all.stream()
                .collect(Collectors.toMap(ResumeExperience::getId, e -> e));

        for (ReorderRequest.ReorderItem item : request.getItems()) {
            ResumeExperience exp = byId.get(item.getId());
            if (exp == null) {
                throw new AccessDeniedException("Запись не принадлежит соискателю");
            }
            exp.setDisplayOrder(item.getDisplayOrder());
        }

        experienceRepository.saveAll(all);
        log.info("Изменён порядок опыта: userId={}, count={}", userId, request.getItems().size());

        return experienceRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private ApplicantProfile getProfile(UUID userId) {
        return applicantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + userId));
    }

    private ResumeExperience getOwnedExperience(UUID userId, UUID experienceId) {
        ResumeExperience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Запись опыта не найдена: " + experienceId));

        if (!experience.getApplicantProfile().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Запись не принадлежит соискателю");
        }
        return experience;
    }

    private void validateDates(String startDate, String endDate) {
        if (endDate != null && endDate.compareTo(startDate) < 0) {
            throw new BusinessException("INVALID_DATE_RANGE",
                    "Дата окончания не может быть раньше даты начала");
        }
    }

    private ExperienceResponse toResponse(ResumeExperience e) {
        return ExperienceResponse.builder()
                .id(e.getId())
                .organization(e.getOrganization())
                .position(e.getPosition())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .description(e.getDescription())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}