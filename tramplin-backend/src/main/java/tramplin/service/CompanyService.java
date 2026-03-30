package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.request.UpdateCompanyRequest;
import tramplin.dto.response.CompanyProfileResponse;
import tramplin.entity.Company;
import tramplin.repository.CompanyRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile(UUID userId) {
        Company company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + userId));

        return toResponse(company);
    }

    @Transactional
    public CompanyProfileResponse updateProfile(UUID userId, UpdateCompanyRequest request) {
        Company company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + userId));

        company.setCompanyName(request.getCompanyName());
        company.setDescription(request.getDescription());
        company.setIndustry(request.getIndustry());
        company.setInn(request.getInn());
        company.setWebsiteUrl(request.getWebsiteUrl());
        company.setCity(request.getCity());
        company.setAddress(request.getAddress());
        company.setPhone(request.getPhone());
        company.setEmail(request.getEmail());
        if (request.getOfficePhotos() != null) {
            company.setOfficePhotos(String.join(",", request.getOfficePhotos()));
        }
        if (request.getVideoUrl() != null) {
            company.setVideoUrl(request.getVideoUrl());
        }

        Company saved = companyRepository.save(company);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse getPublicProfile(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Компания не найдена: " + companyId));

        return CompanyProfileResponse.builder()
                .userId(company.getUser().getId())
                .companyName(company.getCompanyName())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .websiteUrl(company.getWebsiteUrl())
                .logoUrl(company.getLogoUrl())
                .city(company.getCity())
                .address(company.getAddress())
                .officePhotos(company.getOfficePhotos() != null
                        ? List.of(company.getOfficePhotos().split(","))
                        : List.of())
                .videoUrl(company.getVideoUrl())
                .verificationStatus(company.getVerificationStatus().name())
                .build();
    }

    private CompanyProfileResponse toResponse(Company company) {
        return CompanyProfileResponse.builder()
                .userId(company.getUser().getId())
                .companyName(company.getCompanyName())
                .description(company.getDescription())
                .industry(company.getIndustry())
                .inn(company.getInn())
                .websiteUrl(company.getWebsiteUrl())
                .logoUrl(company.getLogoUrl())
                .city(company.getCity())
                .address(company.getAddress())
                .phone(company.getPhone())
                .email(company.getEmail())
                .officePhotos(company.getOfficePhotos() != null
                        ? List.of(company.getOfficePhotos().split(","))
                        : List.of())
                .videoUrl(company.getVideoUrl())
                .verificationStatus(company.getVerificationStatus().name())
                .build();
    }
}