package tramplin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tramplin.dto.request.ChangePasswordRequest;
import tramplin.dto.request.LoginRequest;
import tramplin.dto.request.RefreshTokenRequest;
import tramplin.dto.request.RegisterRequest;
import tramplin.dto.response.AuthResponse;
import tramplin.entity.ApplicantProfile;
import tramplin.entity.Company;
import tramplin.entity.PrivacySettings;
import tramplin.entity.User;
import tramplin.entity.enums.AccountStatus;
import tramplin.entity.enums.Role;
import tramplin.exception.BusinessException;
import tramplin.exception.ConflictException;
import tramplin.repository.ApplicantProfileRepository;
import tramplin.repository.CompanyRepository;
import tramplin.repository.PrivacySettingsRepository;
import tramplin.repository.UserRepository;
import tramplin.security.JwtProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ApplicantProfileRepository applicantProfileRepository;
    private final CompanyRepository companyRepository;
    private final PrivacySettingsRepository privacySettingsRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Пользователь с email " + request.getEmail() + " уже существует");
        }

        Role role = Role.valueOf(request.getRole());
        AccountStatus status = (role == Role.EMPLOYER)
                ? AccountStatus.PENDING_VERIFICATION
                : AccountStatus.ACTIVE;

        User user = User.builder()
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(status)
                .build();

        user = userRepository.save(user);

        if (role == Role.APPLICANT) {
            ApplicantProfile profile = applicantProfileRepository.save(
                    ApplicantProfile.builder()
                            .user(user)
                            .firstName("")
                            .lastName("")
                            .build()
            );
            privacySettingsRepository.save(
                    PrivacySettings.builder()
                            .applicant(profile)
                            .build()
            );
        } else if (role == Role.EMPLOYER) {
            companyRepository.save(
                    Company.builder()
                            .user(user)
                            .companyName("")
                            .build()
            );
        }

        log.info("Зарегистрирован пользователь: email={}, role={}, status={}",
                user.getEmail(), user.getRole(), user.getStatus());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Неверный email или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Неверный email или пароль");
        }

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new BusinessException("ACCOUNT_BLOCKED",
                    "Ваш аккаунт заблокирован. Обратитесь к администратору.");
        }

        log.info("Авторизация: email={}, role={}", user.getEmail(), user.getRole());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Невалидный или истёкший refresh-токен");
        }

        var userId = jwtProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Пользователь не найден"));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new BusinessException("ACCOUNT_BLOCKED", "Аккаунт заблокирован");
        }

        log.info("Обновление токена: email={}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse me(java.util.UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Пользователь не найден"));
        return buildAuthResponse(user);
    }

    @Transactional
    public void changePassword(java.util.UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Пользователь не найден"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Неверный текущий пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Пароль изменён: email={}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
