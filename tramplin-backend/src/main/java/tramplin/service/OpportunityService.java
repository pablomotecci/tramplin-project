package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.opportunity.CreateOpportunityRequest;
import tramplin.dto.opportunity.OpportunityMapResponse;
import tramplin.dto.opportunity.OpportunityResponse;
import tramplin.entity.Company;
import tramplin.entity.Opportunity;
import tramplin.entity.Tag;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.entity.enums.OpportunityType;
import tramplin.entity.enums.VerificationStatus;
import tramplin.entity.enums.WorkFormat;
import tramplin.exception.BusinessException;
import tramplin.repository.CompanyRepository;
import tramplin.repository.OpportunityRepository;
import tramplin.repository.TagRepository;
import tramplin.security.UserPrincipal;
import tramplin.specification.OpportunitySpecification;
import tramplin.dto.opportunity.UpdateOpportunityRequest;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CompanyRepository companyRepository;
    private final YandexGeocoderService yandexGeocoderService;
    private final TagRepository tagRepository;

    @Transactional
    public OpportunityResponse create(CreateOpportunityRequest request, UserPrincipal principal) {
        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        if (company.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException("EMPLOYER_NOT_VERIFIED",
                    "Компания должна пройти верификацию перед публикацией вакансий");
        }

        Opportunity opportunity = Opportunity.builder()
                .employer(company)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .workFormat(request.getWorkFormat())
                .status(OpportunityStatus.ACTIVE)
                .city(request.getCity())
                .address(request.getAddress())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .publishedAt(LocalDateTime.now())
                .expiresAt(request.getExpiresAt())
                .eventDate(request.getEventDate())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .contactUrl(request.getContactUrl())
                .build();

        String fullAddress = request.getCity() + ", " + request.getAddress();
        Point location = yandexGeocoderService.geocode(fullAddress);
        opportunity.setLocation(location); // null если геокодер не нашёл

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
            opportunity.setTags(tags);
        }

        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            opportunity.setMediaUrls(String.join(",", request.getMediaUrls()));
        }

        Opportunity saved = opportunityRepository.save(opportunity);
        log.info("Создана вакансия '{}' компанией {}", saved.getTitle(), company.getCompanyName());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public OpportunityResponse getById(UUID id) {
        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Вакансия не найдена: " + id));

        return mapToResponse(opportunity);
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> getByEmployerId(UUID employerId) {
        return opportunityRepository.findByEmployerId(employerId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> getMyOpportunities(UUID userId, OpportunityStatus status) {
        Company company = companyRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + userId));

        List<Opportunity> opportunities;
        if (status != null) {
            opportunities = opportunityRepository.findByEmployerIdAndStatus(company.getId(), status);
        } else {
            opportunities = opportunityRepository.findByEmployerId(company.getId());
        }
        return opportunities.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public OpportunityResponse changeStatus(UUID id, OpportunityStatus newStatus, UserPrincipal principal) {
        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена: " + id));

        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Компания не найдена"));

        if (!opportunity.getEmployer().getId().equals(company.getId())) {
            throw new BusinessException("FORBIDDEN", "Вы не можете менять статус чужой вакансии");
        }

        if (newStatus == OpportunityStatus.HIDDEN || newStatus == OpportunityStatus.ON_MODERATION) {
            throw new BusinessException("FORBIDDEN", "Этот статус может установить только куратор");
        }

        opportunity.setStatus(newStatus);
        log.info("Статус вакансии '{}' изменён на {}", opportunity.getTitle(), newStatus);
        return mapToResponse(opportunity);
    }

    @Transactional(readOnly = true)
    public List<OpportunityMapResponse> getForMap(double swLat, double swLng, double neLat, double neLng, List<UUID> tagIds) {
        List<Opportunity> opportunities;
        if (tagIds != null && !tagIds.isEmpty()) {
            opportunities = opportunityRepository.findInBoundingBoxWithTags(swLng, swLat, neLng, neLat, tagIds);
        } else {
            opportunities = opportunityRepository.findInBoundingBox(swLng, swLat, neLng, neLat);
        }
        return opportunities.stream()
                .map(this::mapToMapResponse)
                .toList();
    }

    private OpportunityMapResponse mapToMapResponse(Opportunity entity) {
        return OpportunityMapResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .companyName(entity.getEmployer().getCompanyName())
                .logoUrl(entity.getEmployer().getLogoUrl())
                .type(entity.getType())
                .workFormat(entity.getWorkFormat())
                .city(entity.getCity())
                .latitude(entity.getLocation() != null ? entity.getLocation().getY() : null)
                .longitude(entity.getLocation() != null ? entity.getLocation().getX() : null)
                .salaryMin(entity.getSalaryMin())
                .salaryMax(entity.getSalaryMax())
                .tags(entity.getTags().stream().map(Tag::getName).limit(3).toList())
                .build();
    }

    @Transactional
    public OpportunityResponse update(UUID id, UpdateOpportunityRequest request, UserPrincipal principal) {
        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена: " + id));

        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        if (!opportunity.getEmployer().getId().equals(company.getId())) {
            throw new BusinessException("FORBIDDEN", "Вы не можете редактировать чужую вакансию");
        }

        if (request.getTitle() != null)        opportunity.setTitle(request.getTitle());
        if (request.getDescription() != null)  opportunity.setDescription(request.getDescription());
        if (request.getType() != null)         opportunity.setType(request.getType());
        if (request.getWorkFormat() != null)   opportunity.setWorkFormat(request.getWorkFormat());
        if (request.getCity() != null)         opportunity.setCity(request.getCity());
        if (request.getAddress() != null)      opportunity.setAddress(request.getAddress());
        if (request.getSalaryMin() != null)    opportunity.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null)    opportunity.setSalaryMax(request.getSalaryMax());
        if (request.getExpiresAt() != null)    opportunity.setExpiresAt(request.getExpiresAt());
        if (request.getEventDate() != null)    opportunity.setEventDate(request.getEventDate());
        if (request.getContactEmail() != null) opportunity.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) opportunity.setContactPhone(request.getContactPhone());
        if (request.getContactUrl() != null)   opportunity.setContactUrl(request.getContactUrl());

        if (request.getCity() != null || request.getAddress() != null) {
            String fullAddress = opportunity.getCity() + ", " + opportunity.getAddress();
            Point location = yandexGeocoderService.geocode(fullAddress);
            if (location != null) opportunity.setLocation(location);
        }

        if (request.getTagIds() != null) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
            opportunity.setTags(tags);
        }

        if (request.getMediaUrls() != null) {
            opportunity.setMediaUrls(String.join(",", request.getMediaUrls()));
        }

        Opportunity saved = opportunityRepository.save(opportunity);
        log.info("Обновлена вакансия '{}' компанией {}", saved.getTitle(), company.getCompanyName());
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(UUID id, UserPrincipal principal) {
        Opportunity opportunity = opportunityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена: " + id));

        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        if (!opportunity.getEmployer().getId().equals(company.getId())) {
            throw new BusinessException("FORBIDDEN", "Вы не можете удалить чужую вакансию");
        }

        opportunityRepository.delete(opportunity);
        log.info("Удалена вакансия '{}' компанией {}", opportunity.getTitle(), company.getCompanyName());
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> getAll(
            OpportunityType type,
            WorkFormat workFormat,
            String city,
            Long salaryMin,
            List<UUID> tagIds,
            Pageable pageable
    ) {
        Specification<Opportunity> spec = Specification
                .where(OpportunitySpecification.hasStatus(OpportunityStatus.ACTIVE))
                .and(OpportunitySpecification.hasType(type))
                .and(OpportunitySpecification.hasWorkFormat(workFormat))
                .and(OpportunitySpecification.hasCity(city))
                .and(OpportunitySpecification.hasSalaryMin(salaryMin))
                .and(OpportunitySpecification.hasTags(tagIds));

        return opportunityRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    private OpportunityResponse mapToResponse(Opportunity entity) {
        return OpportunityResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .type(entity.getType())
                .workFormat(entity.getWorkFormat())
                .status(entity.getStatus())
                .city(entity.getCity())
                .address(entity.getAddress())
                .latitude(entity.getLocation() != null ? entity.getLocation().getY() : null)
                .longitude(entity.getLocation() != null ? entity.getLocation().getX() : null)
                .salaryMin(entity.getSalaryMin())
                .salaryMax(entity.getSalaryMax())
                .publishedAt(entity.getPublishedAt())
                .expiresAt(entity.getExpiresAt())
                .eventDate(entity.getEventDate())
                .contactEmail(entity.getContactEmail())
                .contactPhone(entity.getContactPhone())
                .contactUrl(entity.getContactUrl())
                .employerId(entity.getEmployer().getId())
                .companyName(entity.getEmployer().getCompanyName())
                .logoUrl(entity.getEmployer().getLogoUrl())
                .tags(entity.getTags().stream().map(Tag::getName).toList())
                .mediaUrls(entity.getMediaUrls() != null
                        ? List.of(entity.getMediaUrls().split(","))
                        : List.of())
                .build();
    }
}