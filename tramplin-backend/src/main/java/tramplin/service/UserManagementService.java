package tramplin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.user.CreateCuratorDto;
import tramplin.dto.user.ResetPasswordResponseDto;
import tramplin.dto.user.UserManagementResponseDto;
import tramplin.entity.User;
import tramplin.entity.enums.AccountStatus;
import tramplin.entity.enums.Role;
import tramplin.exception.BusinessException;
import tramplin.exception.ConflictException;
import tramplin.repository.UserRepository;

import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserManagementResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserManagementResponseDto> getUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserManagementResponseDto> getUsersByStatus(AccountStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserManagementResponseDto> getUsersByRoleAndStatus(Role role, AccountStatus status, Pageable pageable) {
        return userRepository.findByRoleAndStatus(role, status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public UserManagementResponseDto createCurator(CreateCuratorDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Пользователь с email '" + dto.getEmail() + "' уже существует");
        }

        User curator = User.builder()
                .email(dto.getEmail())
                .displayName(dto.getDisplayName())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(Role.CURATOR)
                .status(AccountStatus.ACTIVE)
                .build();

        User saved = userRepository.save(curator);
        log.info("Создан новый куратор: {} ({})", saved.getDisplayName(), saved.getEmail());
        return mapToResponse(saved);
    }

    @Transactional
    public ResetPasswordResponseDto resetPassword(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException("ADMIN_PASSWORD_RESET", "Невозможно сбросить пароль администратора");
        }

        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        log.info("Пароль сброшен для пользователя: {} ({})", user.getDisplayName(), user.getEmail());

        return ResetPasswordResponseDto.builder()
                .userId(user.getId())
                .temporaryPassword(tempPassword)
                .build();
    }

    @Transactional
    public UserManagementResponseDto changeStatus(UUID userId, AccountStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException("ADMIN_STATUS_CHANGE", "Невозможно изменить статус администратора");
        }

        user.setStatus(newStatus);
        userRepository.save(user);

        log.info("Статус пользователя {} ({}) изменён на {}", user.getDisplayName(), user.getEmail(), newStatus);
        return mapToResponse(user);
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private UserManagementResponseDto mapToResponse(User user) {
        return UserManagementResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}