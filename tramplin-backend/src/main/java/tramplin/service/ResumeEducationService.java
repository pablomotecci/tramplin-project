package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.resume.CreateEducationRequest;
import tramplin.dto.resume.EducationResponse;
import tramplin.dto.resume.ReorderRequest;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.ResumeEducation;
import tramplin.entity.enums.Degree;
import tramplin.exception.BusinessException;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.ResumeEducationRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeEducationService {

    private static final int MAX_ITEMS = 5;

    private final ResumeEducationRepository educationRepository;
    private final ApplicantProfileRepository applicantProfileRepository;

    @Transactional(readOnly = true)
    public List<EducationResponse> list(UUID userId) {
        ApplicantProfile profile = getProfile(userId);
        return educationRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EducationResponse create(UUID userId, CreateEducationRequest request) {
        ApplicantProfile profile = getProfile(userId);
        validateYears(request.getStartYear(), request.getEndYear());

        long count = educationRepository.countByApplicantProfileId(profile.getId());
        if (count >= MAX_ITEMS) {
            throw new BusinessException("LIMIT_EXCEEDED",
                    "Превышен лимит образования: до " + MAX_ITEMS);
        }

        ResumeEducation education = ResumeEducation.builder()
                .applicantProfile(profile)
                .institution(request.getInstitution())
                .faculty(request.getFaculty())
                .degree(Degree.valueOf(request.getDegree()))
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .description(request.getDescription())
                .displayOrder((int) count)
                .build();

        ResumeEducation saved = educationRepository.save(education);
        log.info("Создана запись образования: id={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public EducationResponse update(UUID userId, UUID educationId, CreateEducationRequest request) {
        ResumeEducation education = getOwnedEducation(userId, educationId);
        validateYears(request.getStartYear(), request.getEndYear());

        education.setInstitution(request.getInstitution());
        education.setFaculty(request.getFaculty());
        education.setDegree(Degree.valueOf(request.getDegree()));
        education.setStartYear(request.getStartYear());
        education.setEndYear(request.getEndYear());
        education.setDescription(request.getDescription());

        ResumeEducation saved = educationRepository.save(education);
        log.info("Обновлена запись образования: id={}, userId={}", saved.getId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID userId, UUID educationId) {
        ResumeEducation education = getOwnedEducation(userId, educationId);
        educationRepository.delete(education);
        log.info("Удалена запись образования: id={}, userId={}", educationId, userId);
    }

    @Transactional
    public List<EducationResponse> reorder(UUID userId, ReorderRequest request) {
        ApplicantProfile profile = getProfile(userId);
        List<ResumeEducation> all = educationRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId());

        Map<UUID, ResumeEducation> byId = all.stream()
                .collect(Collectors.toMap(ResumeEducation::getId, e -> e));

        for (ReorderRequest.ReorderItem item : request.getItems()) {
            ResumeEducation e = byId.get(item.getId());
            if (e == null) {
                throw new AccessDeniedException("Запись не принадлежит соискателю");
            }
            e.setDisplayOrder(item.getDisplayOrder());
        }

        educationRepository.saveAll(all);
        log.info("Изменён порядок образования: userId={}", userId);

        return educationRepository.findByApplicantProfileIdOrderByDisplayOrder(profile.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }


    private ApplicantProfile getProfile(UUID userId) {
        return applicantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден: " + userId));
    }

    private ResumeEducation getOwnedEducation(UUID userId, UUID educationId) {
        ResumeEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Запись образования не найдена: " + educationId));

        if (!education.getApplicantProfile().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Запись не принадлежит соискателю");
        }
        return education;
    }

    private void validateYears(Integer startYear, Integer endYear) {
        if (endYear != null && endYear < startYear) {
            throw new BusinessException("INVALID_YEAR_RANGE",
                    "Год окончания не может быть раньше года начала");
        }
    }

    private EducationResponse toResponse(ResumeEducation e) {
        return EducationResponse.builder()
                .id(e.getId())
                .institution(e.getInstitution())
                .faculty(e.getFaculty())
                .degree(e.getDegree().name())
                .startYear(e.getStartYear())
                .endYear(e.getEndYear())
                .description(e.getDescription())
                .displayOrder(e.getDisplayOrder())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}