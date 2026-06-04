package tramplin.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ParseResumeTagsRequest {

    @NotBlank(message = "Текст резюме обязателен")
    @Size(max = 10000, message = "Текст резюме слишком длинный (максимум 10000 символов)")
    private String text;
}