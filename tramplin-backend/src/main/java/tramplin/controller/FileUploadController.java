package tramplin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tramplin.dto.response.ApiResponse;
import tramplin.service.FileUploadService;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Загрузка файлов", description = "Загрузка аватаров, логотипов и фото")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить файл",
               description = "Загрузка изображения. Поддерживаемые форматы: JPEG, PNG, GIF, WebP. Макс. 10 МБ.")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "general") String type
    ) {
        String url = fileUploadService.upload(file, type);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("url", url)));
    }
}