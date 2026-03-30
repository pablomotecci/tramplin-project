package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tramplin.dto.request.ChangePasswordRequest;
import tramplin.dto.request.LoginRequest;
import tramplin.dto.request.RefreshTokenRequest;
import tramplin.dto.request.RegisterRequest;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.AuthResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.AuthService;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, авторизация, обновление токена")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация", description = "Создание нового пользователя (соискатель или работодатель)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 — ресурс создан
                .body(ApiResponse.ok(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Авторизация", description = "Вход по email и паролю, получение JWT-токенов")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена", description = "Получение новой пары JWT через refresh-токен")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь", description = "Данные авторизованного пользователя по JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> me(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
           tramplin.security.UserPrincipal principal
    ) {
        AuthResponse response = authService.me(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Смена пароля", description = "Смена пароля авторизованным пользователем")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
