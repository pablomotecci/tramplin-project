package tramplin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tramplin.exception.BusinessException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    public String upload(MultipartFile file, String subfolder) {
        if (file.isEmpty()) {
            throw new BusinessException("EMPTY_FILE", "Файл пустой");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException("INVALID_FILE_TYPE",
                    "Допустимые форматы: JPEG, PNG, GIF, WebP");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "Максимальный размер файла: 10 МБ");
        }

        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;

        try {
            Path dir = Paths.get(uploadDir, subfolder);
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Файл загружен: {}", target);
            return "/uploads/" + subfolder + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("UPLOAD_ERROR", "Ошибка при сохранении файла");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}