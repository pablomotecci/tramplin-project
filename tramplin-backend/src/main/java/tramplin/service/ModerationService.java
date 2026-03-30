package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.moderation.ModerationActionDto;
import tramplin.dto.moderation.ModerationLogResponseDto;
import tramplin.entity.ModerationLog;
import tramplin.entity.Opportunity;
import tramplin.entity.User;
import tramplin.entity.enums.AccountStatus;
import tramplin.entity.enums.ModerationAction;
import tramplin.entity.enums.OpportunityStatus;
import tramplin.entity.enums.TargetType;
import tramplin.exception.BusinessException;
import tramplin.repository.CompanyRepository;
import tramplin.repository.ModerationLogRepository;
import tramplin.repository.OpportunityRepository;
import tramplin.repository.UserRepository;
import tramplin.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationLogRepository moderationLogRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public ModerationLogResponseDto hideOpportunity(UUID opportunityId, ModerationActionDto dto, UserPrincipal curator) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена: " + opportunityId));

        if (opportunity.getStatus() == OpportunityStatus.HIDDEN) {
            throw new BusinessException("ALREADY_HIDDEN", "Вакансия уже скрыта");
        }

        opportunity.setStatus(OpportunityStatus.HIDDEN);
        opportunityRepository.save(opportunity);

        User curatorUser = findCurator(curator.getUserId());
        String reason = dto != null ? dto.getReason() : null;
        String details = dto != null ? dto.getDetails() : null;

        log.info("Куратор {} скрыл вакансию {}", curator.getUserId(), opportunityId);
        return createLog(curatorUser, ModerationAction.HIDE, TargetType.OPPORTUNITY, opportunityId, reason, details);
    }

    @Transactional
    public ModerationLogResponseDto unhideOpportunity(UUID opportunityId, ModerationActionDto dto, UserPrincipal curator) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new EntityNotFoundException("Вакансия не найдена: " + opportunityId));

        if (opportunity.getStatus() != OpportunityStatus.HIDDEN) {
            throw new BusinessException("NOT_HIDDEN", "Вакансия не скрыта");
        }

        opportunity.setStatus(OpportunityStatus.ACTIVE);
        opportunityRepository.save(opportunity);

        User curatorUser = findCurator(curator.getUserId());
        String reason = dto != null ? dto.getReason() : null;
        String details = dto != null ? dto.getDetails() : null;

        log.info("Куратор {} восстановил вакансию {}", curator.getUserId(), opportunityId);
        return createLog(curatorUser, ModerationAction.UNHIDE, TargetType.OPPORTUNITY, opportunityId, reason, details);
    }

    @Transactional
    public ModerationLogResponseDto blockUser(UUID userId, ModerationActionDto dto, UserPrincipal curator) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new BusinessException("ALREADY_BLOCKED", "Пользователь уже заблокирован");
        }

        if (dto == null || dto.getReason() == null || dto.getReason().isBlank()) {
            throw new BusinessException("REASON_REQUIRED", "Причина блокировки обязательна");
        }

        user.setStatus(AccountStatus.BLOCKED);
        userRepository.save(user);

        User curatorUser = findCurator(curator.getUserId());

        log.info("Куратор {} заблокировал пользователя {}", curator.getUserId(), userId);
        return createLog(curatorUser, ModerationAction.BLOCK_USER, TargetType.USER, userId, dto.getReason(), dto.getDetails());
    }

    @Transactional
    public ModerationLogResponseDto unblockUser(UUID userId, ModerationActionDto dto, UserPrincipal curator) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (user.getStatus() != AccountStatus.BLOCKED) {
            throw new BusinessException("NOT_BLOCKED", "Пользователь не заблокирован");
        }

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        User curatorUser = findCurator(curator.getUserId());
        String reason = dto != null ? dto.getReason() : null;
        String details = dto != null ? dto.getDetails() : null;

        log.info("Куратор {} разблокировал пользователя {}", curator.getUserId(), userId);
        return createLog(curatorUser, ModerationAction.UNBLOCK_USER, TargetType.USER, userId, reason, details);
    }

    @Transactional(readOnly = true)
    public Page<ModerationLogResponseDto> getLogs(Pageable pageable) {
        return moderationLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ModerationLogResponseDto> getLogsByTarget(TargetType targetType, UUID targetId) {
        return moderationLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private User findCurator(UUID curatorId) {
        return userRepository.findById(curatorId)
                .orElseThrow(() -> new EntityNotFoundException("Куратор не найден: " + curatorId));
    }

    private ModerationLogResponseDto createLog(User curator, ModerationAction action, TargetType targetType,
                                                UUID targetId, String reason, String details) {
        ModerationLog logEntry = ModerationLog.builder()
                .curator(curator)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .details(details)
                .build();

        ModerationLog saved = moderationLogRepository.save(logEntry);
        return mapToResponse(saved);
    }

    private ModerationLogResponseDto mapToResponse(ModerationLog log) {
        return ModerationLogResponseDto.builder()
                .id(log.getId())
                .curatorId(log.getCurator().getId())
                .curatorName(log.getCurator().getDisplayName())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .reason(log.getReason())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}