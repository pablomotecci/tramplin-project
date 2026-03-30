package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.response.FavoriteResponse;
import tramplin.security.UserPrincipal;
import tramplin.service.FavoriteService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Избранное", description = "Добавление и просмотр избранных вакансий")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{opportunityId}")
    @Operation(summary = "Добавить в избранное")
    public ResponseEntity<ApiResponse<FavoriteResponse>> add(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID opportunityId
    ) {
        FavoriteResponse response = favoriteService.addFavorite(principal.getUserId(), opportunityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @DeleteMapping("/{opportunityId}")
    @Operation(summary = "Удалить из избранного")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID opportunityId
    ) {
        favoriteService.removeFavorite(principal.getUserId(), opportunityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Мои избранные", description = "Список избранных вакансий текущего пользователя")
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getMyFavorites(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<FavoriteResponse> response = favoriteService.getMyFavorites(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}