package tramplin.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateExperienceRequest {

    @NotBlank(message = "Название организации обязательно")
    @Size(max = 255)
    private String organization;

    @NotBlank(message = "Должность обязательна")
    @Size(max = 255)
    private String position;

    @NotBlank(message = "Дата начала обязательна")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Формат даты: YYYY-MM")
    private String startDate;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Формат даты: YYYY-MM")
    private String endDate;

    @Size(max = 2000)
    private String description;
}