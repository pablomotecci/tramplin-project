package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.application.ApplicationResponse;
import tramplin.dto.request.UpdateApplicantRequest;
import tramplin.dto.response.ApplicantProfileResponse;
import tramplin.dto.response.ContactResponse;
import tramplin.dto.tag.TagResponse;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Application;
import tramplin.entity.ContactRequest;
import tramplin.entity.PrivacySettings;
import tramplin.entity.Tag;
import tramplin.entity.User;
import tramplin.entity.enums.ContactRequestStatus;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.ApplicationRepository;
import tramplin.repository.ContactRequestRepository;
import tramplin.repository.PrivacySettingsRepository;
import tramplin.repository.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicantProfileService {

    private final ApplicantProfileRepository applicantProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final PrivacySettingsRepository privacySettingsRepository;
    private final PrivacyService privacyService;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public ApplicantProfileResponse getProfile(UUID userId) {
        ApplicantProfile profile = applicantProfileRepository.findByUserIdWithTags(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + userId));

        return toResponse(profile);
    }

    @Transactional
    public ApplicantProfileResponse updateProfile(UUID userId, UpdateApplicantRequest request) {
        ApplicantProfile profile = applicantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + userId));

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setMiddleName(request.getMiddleName());
        profile.setUniversity(request.getUniversity());
        profile.setCourse(request.getCourse());
        profile.setGraduationYear(request.getGraduationYear());
        profile.setBio(request.getBio());
        profile.setPhone(request.getPhone());
        profile.setPortfolioUrl(request.getPortfolioUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setSkillsSummary(request.getSkillsSummary());

        ApplicantProfile saved = applicantProfileRepository.save(profile);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTags(UUID userId) {
        ApplicantProfile profile = applicantProfileRepository.findByUserIdWithTags(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + userId));

        return profile.getTags().stream()
                .map(this::mapTagToResponse)
                .toList();
    }

    @Transactional
    public List<TagResponse> updateTags(UUID userId, Set<UUID> tagIds) {
        ApplicantProfile profile = applicantProfileRepository.findByUserIdWithTags(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + userId));

        Set<Tag> tags = new HashSet<>(tagRepository.findAllById(tagIds));
        profile.setTags(tags);
        applicantProfileRepository.save(profile);

        return tags.stream()
                .map(this::mapTagToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApplicantProfileResponse getPublicProfile(UUID profileUserId, UUID viewerId, String viewerRole) {
        ApplicantProfileResponse response = getProfile(profileUserId);

        PrivacySettings privacy = privacySettingsRepository.findByApplicantUserId(profileUserId)
                .orElse(null);

        if (privacy == null) return response;

        if (!privacyService.canView(privacy.getProfileVisibility(), viewerId, viewerRole, profileUserId)) {
            throw new EntityNotFoundException("Профиль не найден или скрыт");
        }
        if (!privacyService.canView(privacy.getResumeVisibility(), viewerId, viewerRole, profileUserId)) {
            response.setBio(null);
            response.setPhone(null);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getPublicApplications(UUID profileUserId, UUID viewerId, String viewerRole) {
        PrivacySettings privacy = privacySettingsRepository.findByApplicantUserId(profileUserId).orElse(null);

        if (privacy != null && !privacyService.canView(privacy.getApplicationsVisibility(), viewerId, viewerRole, profileUserId)) {
            return List.of();
        }

        return applicationRepository.findByApplicantUserIdWithDetails(profileUserId).stream()
                .map(this::mapApplicationToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getPublicContacts(UUID profileUserId, UUID viewerId, String viewerRole) {
        PrivacySettings privacy = privacySettingsRepository.findByApplicantUserId(profileUserId).orElse(null);

        if (privacy != null && !privacyService.canView(privacy.getContactsVisibility(), viewerId, viewerRole, profileUserId)) {
            return List.of();
        }

        return contactRequestRepository.findAllByUserIdAndStatus(profileUserId, ContactRequestStatus.ACCEPTED).stream()
                .map(cr -> mapContactToResponse(cr, profileUserId))
                .toList();
    }

    private ApplicationResponse mapApplicationToResponse(Application app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .status(app.getStatus())
                .coverLetter(app.getCoverLetter())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .opportunityId(app.getOpportunity().getId())
                .opportunityTitle(app.getOpportunity().getTitle())
                .companyName(app.getOpportunity().getEmployer().getCompanyName())
                .applicantId(app.getApplicant().getId())
                .applicantFirstName(app.getApplicant().getFirstName())
                .applicantLastName(app.getApplicant().getLastName())
                .applicantEmail(app.getApplicant().getUser().getEmail())
                .build();
    }

    private ContactResponse mapContactToResponse(ContactRequest cr, UUID profileUserId) {
        User other = cr.getSender().getId().equals(profileUserId)
                ? cr.getReceiver()
                : cr.getSender();

        return ContactResponse.builder()
                .contactRequestId(cr.getId())
                .userId(other.getId())
                .displayName(other.getDisplayName())
                .email(other.getEmail())
                .connectedAt(cr.getUpdatedAt())
                .build();
    }

    private TagResponse mapTagToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .category(tag.getCategory())
                .approved(tag.isApproved())
                .parentId(tag.getParent() != null ? tag.getParent().getId() : null)
                .parentName(tag.getParent() != null ? tag.getParent().getName() : null)
                .build();
    }

    private ApplicantProfileResponse toResponse(ApplicantProfile profile) {
        return ApplicantProfileResponse.builder()
                .userId(profile.getUser().getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .middleName(profile.getMiddleName())
                .university(profile.getUniversity())
                .course(profile.getCourse())
                .graduationYear(profile.getGraduationYear())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .phone(profile.getPhone())
                .portfolioUrl(profile.getPortfolioUrl())
                .githubUrl(profile.getGithubUrl())
                .skillsSummary(profile.getSkillsSummary())
                .tags(profile.getTags() != null
                        ? profile.getTags().stream()
                            .map(tag -> tag.getName())
                            .toList()
                        : List.of())
                .build();
    }
}