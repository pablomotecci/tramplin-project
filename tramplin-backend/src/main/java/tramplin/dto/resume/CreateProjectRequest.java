package tramplin.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank(message = "Название проекта обязательно")
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String role;

    @NotBlank(message = "Дата начала обязательна")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Формат даты: YYYY-MM")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Формат даты: YYYY-MM")
    private String endDate;

    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String projectUrl;

    @Size(max = 500)
    private String repositoryUrl;
}