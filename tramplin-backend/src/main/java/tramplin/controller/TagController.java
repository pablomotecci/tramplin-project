package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tramplin.dto.response.ApiResponse;
import tramplin.dto.tag.SuggestTagRequest;
import tramplin.dto.tag.TagResponse;
import tramplin.dto.tag.TagTreeResponse;
import tramplin.entity.enums.TagCategory;
import tramplin.security.UserPrincipal;
import tramplin.service.TagService;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Tag(name = "Теги", description = "Управление тегами: поиск, дерево, предложение новых")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Список тегов", description = "Фильтрация по категории или поиск по подстроке. Без параметров — все одобренные теги")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAll(
            @RequestParam(required = false) TagCategory category,
            @RequestParam(required = false) String search
    ) {
        List<TagResponse> response;
        if (search != null) {
            response = tagService.search(search);
        } else if (category != null) {
            response = tagService.getByCategory(category);
        } else {
            response = tagService.getAllApproved();
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/tree")
    @Operation(summary = "Дерево тегов", description = "Иерархическая структура одобренных тегов")
    public ResponseEntity<ApiResponse<List<TagTreeResponse>>> getTree() {
        List<TagTreeResponse> response = tagService.getTree();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/suggest")
    @PreAuthorize("hasRole('EMPLOYER')")
    @Operation(summary = "Предложить новый тег", description = "Работодатель предлагает тег — он появится после одобрения куратором")
    public ResponseEntity<ApiResponse<TagResponse>> suggest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SuggestTagRequest request
    ) {
        TagResponse response = tagService.suggest(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}