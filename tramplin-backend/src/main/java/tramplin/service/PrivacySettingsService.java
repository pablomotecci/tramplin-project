package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.privacy.PrivacySettingsDto;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.PrivacySettings;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.PrivacySettingsRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacySettingsService {

    private final PrivacySettingsRepository privacySettingsRepository;
    private final ApplicantProfileRepository applicantProfileRepository;

    @Transactional(readOnly = true)
    public PrivacySettingsDto getSettings(UUID userId) {
        ApplicantProfile profile = findProfile(userId);
        PrivacySettings settings = privacySettingsRepository.findByApplicantId(profile.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Настройки приватности не найдены для userId: " + userId));
        return mapToDto(settings);
    }

    @Transactional
    public PrivacySettingsDto updateSettings(UUID userId, PrivacySettingsDto dto) {
        ApplicantProfile profile = findProfile(userId);
        PrivacySettings settings = privacySettingsRepository.findByApplicantId(profile.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Настройки приватности не найдены для userId: " + userId));

        if (dto.getProfileVisibility() != null) {
            settings.setProfileVisibility(dto.getProfileVisibility());
        }
        if (dto.getResumeVisibility() != null) {
            settings.setResumeVisibility(dto.getResumeVisibility());
        }
        if (dto.getApplicationsVisibility() != null) {
            settings.setApplicationsVisibility(dto.getApplicationsVisibility());
        }
        if (dto.getContactsVisibility() != null) {
            settings.setContactsVisibility(dto.getContactsVisibility());
        }

        PrivacySettings saved = privacySettingsRepository.save(settings);
        log.info("Обновлены настройки приватности для userId: {}", userId);
        return mapToDto(saved);
    }

    private ApplicantProfile findProfile(UUID userId) {
        return applicantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль соискателя не найден для userId: " + userId));
    }

    private PrivacySettingsDto mapToDto(PrivacySettings settings) {
        return PrivacySettingsDto.builder()
                .profileVisibility(settings.getProfileVisibility())
                .resumeVisibility(settings.getResumeVisibility())
                .applicationsVisibility(settings.getApplicationsVisibility())
                .contactsVisibility(settings.getContactsVisibility())
                .build();
    }
}