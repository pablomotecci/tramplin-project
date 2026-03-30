package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.verification.CreateVerificationRequestDto;
import tramplin.dto.verification.RejectVerificationDto;
import tramplin.dto.verification.VerificationRequestResponseDto;
import tramplin.entity.Company;
import tramplin.entity.User;
import tramplin.entity.VerificationRequest;
import tramplin.entity.enums.AccountStatus;
import tramplin.entity.enums.VerificationRequestStatus;
import tramplin.entity.enums.VerificationStatus;
import tramplin.exception.BusinessException;
import tramplin.exception.ConflictException;
import tramplin.repository.CompanyRepository;
import tramplin.repository.UserRepository;
import tramplin.repository.VerificationRequestRepository;
import tramplin.security.UserPrincipal;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRequestRepository verificationRequestRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    private static final Set<String> PUBLIC_EMAIL_DOMAINS = Set.of(
            "gmail.com", "mail.ru", "yandex.ru", "ya.ru", "inbox.ru",
            "list.ru", "bk.ru", "internet.ru", "rambler.ru", "outlook.com",
            "hotmail.com", "yahoo.com", "icloud.com", "protonmail.com"
    );

    @Transactional
    public VerificationRequestResponseDto createRequest(UserPrincipal principal,
                                                        CreateVerificationRequestDto dto) {
        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        boolean hasActive = verificationRequestRepository.existsByEmployerIdAndStatusIn(
                company.getId(),
                List.of(VerificationRequestStatus.PENDING,
                        VerificationRequestStatus.INN_VERIFIED,
                        VerificationRequestStatus.EMAIL_VERIFIED));
        if (hasActive) {
            throw new ConflictException("У вас уже есть активная заявка на верификацию");
        }

        VerificationRequest request = VerificationRequest.builder()
                .employer(company)
                .inn(dto.getInn())
                .companyDomain(dto.getCompanyDomain())
                .corporateEmail(dto.getCorporateEmail())
                .build();

        if (!validateInn(dto.getInn())) {
            request.setStatus(VerificationRequestStatus.REJECTED);
            request.setRejectionReason("ИНН не прошёл валидацию: должен содержать 10 или 12 цифр");
            VerificationRequest saved = verificationRequestRepository.save(request);
            log.warn("Заявка на верификацию отклонена — невалидный ИНН: {}", dto.getInn());
            return mapToResponse(saved);
        }
        request.setStatus(VerificationRequestStatus.INN_VERIFIED);

        if (!validateCorporateEmail(dto.getCorporateEmail(), dto.getCompanyDomain())) {
            request.setStatus(VerificationRequestStatus.REJECTED);
            request.setRejectionReason(
                    "Корпоративный email не прошёл проверку: домен почты должен совпадать с доменом компании и не быть публичным");
            VerificationRequest saved = verificationRequestRepository.save(request);
            log.warn("Заявка на верификацию отклонена — невалидный email: {}", dto.getCorporateEmail());
            return mapToResponse(saved);
        }
        request.setStatus(VerificationRequestStatus.EMAIL_VERIFIED);

        company.setVerificationStatus(VerificationStatus.PENDING);
        companyRepository.save(company);

        VerificationRequest saved = verificationRequestRepository.save(request);
        log.info("Заявка на верификацию создана для компании '{}', статус: {}",
                company.getCompanyName(), saved.getStatus());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public VerificationRequestResponseDto getMyRequest(UserPrincipal principal) {
        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        VerificationRequest request = verificationRequestRepository
                .findTopByEmployerIdOrderByCreatedAtDesc(company.getId())
                .orElseThrow(() -> new EntityNotFoundException("Заявка на верификацию не найдена"));

        return mapToResponse(request);
    }

    @Transactional(readOnly = true)
    public List<VerificationRequestResponseDto> getMyRequestHistory(UserPrincipal principal) {
        Company company = companyRepository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Профиль компании не найден для userId: " + principal.getUserId()));

        return verificationRequestRepository.findByEmployerIdOrderByCreatedAtDesc(company.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<VerificationRequestResponseDto> getPendingRequests(Pageable pageable) {
        return verificationRequestRepository
                .findByStatus(VerificationRequestStatus.EMAIL_VERIFIED, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public VerificationRequestResponseDto approve(UUID requestId, UserPrincipal curator) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена: " + requestId));

        if (request.getStatus() == VerificationRequestStatus.APPROVED
                || request.getStatus() == VerificationRequestStatus.REJECTED) {
            throw new BusinessException("INVALID_STATUS",
                    "Нельзя одобрить уже обработанную заявку");
        }

        User curatorUser = userRepository.findById(curator.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Куратор не найден: " + curator.getUserId()));

        request.setStatus(VerificationRequestStatus.APPROVED);
        request.setReviewedBy(curatorUser);
        verificationRequestRepository.save(request);

        Company company = request.getEmployer();
        company.setVerificationStatus(VerificationStatus.VERIFIED);
        companyRepository.save(company);

        User employerUser = company.getUser();
        employerUser.setStatus(AccountStatus.ACTIVE);
        userRepository.save(employerUser);

        log.info("Заявка {} одобрена куратором {}. Компания '{}' верифицирована",
                requestId, curator.getUserId(), company.getCompanyName());
        return mapToResponse(request);
    }

    @Transactional
    public VerificationRequestResponseDto reject(UUID requestId, RejectVerificationDto dto,
                                                 UserPrincipal curator) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена: " + requestId));

        if (request.getStatus() == VerificationRequestStatus.APPROVED
                || request.getStatus() == VerificationRequestStatus.REJECTED) {
            throw new BusinessException("INVALID_STATUS",
                    "Нельзя отклонить уже обработанную заявку");
        }

        User curatorUser = userRepository.findById(curator.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Куратор не найден: " + curator.getUserId()));

        request.setStatus(VerificationRequestStatus.REJECTED);
        request.setRejectionReason(dto.getRejectionReason());
        request.setReviewedBy(curatorUser);
        verificationRequestRepository.save(request);

        Company company = request.getEmployer();
        company.setVerificationStatus(VerificationStatus.UNVERIFIED);
        companyRepository.save(company);

        log.info("Заявка {} отклонена куратором {}. Причина: {}",
                requestId, curator.getUserId(), dto.getRejectionReason());
        return mapToResponse(request);
    }

    private boolean validateInn(String inn) {
        if (inn == null) return false;
        return (inn.length() == 10 || inn.length() == 12) && inn.chars().allMatch(Character::isDigit);
    }

    private boolean validateCorporateEmail(String email, String companyDomain) {
        if (email == null || companyDomain == null) return false;

        String emailDomain = email.substring(email.indexOf('@') + 1).toLowerCase();

        if (PUBLIC_EMAIL_DOMAINS.contains(emailDomain)) return false;

        String normalizedCompanyDomain = companyDomain.toLowerCase()
                .replaceFirst("^(https?://)", "")
                .replaceFirst("^www\\.", "")
                .replaceFirst("/.*$", "");

        return emailDomain.equals(normalizedCompanyDomain);
    }

    private VerificationRequestResponseDto mapToResponse(VerificationRequest request) {
        return VerificationRequestResponseDto.builder()
                .id(request.getId())
                .employerId(request.getEmployer().getId())
                .companyName(request.getEmployer().getCompanyName())
                .inn(request.getInn())
                .companyDomain(request.getCompanyDomain())
                .corporateEmail(request.getCorporateEmail())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .build();
    }
}