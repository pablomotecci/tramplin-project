package tramplin.dto.resume;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEducationRequest {

    @NotBlank(message = "Учебное заведение обязательно")
    @Size(max = 255)
    private String institution;

    @Size(max = 255)
    private String faculty;

    @NotBlank(message = "Уровень образования обязателен")
    @Pattern(regexp = "BACHELOR|MASTER|SPECIALIST|PHD|COLLEGE|OTHER",
            message = "Допустимые значения: BACHELOR, MASTER, SPECIALIST, PHD, COLLEGE, OTHER")
    private String degree;

    @NotNull(message = "Год начала обязателен")
    @Min(value = 1950, message = "Год начала не может быть раньше 1950")
    @Max(value = 2100, message = "Год начала не может быть позже 2100")
    private Integer startYear;

    @Min(value = 1950, message = "Год окончания не может быть раньше 1950")
    @Max(value = 2100, message = "Год окончания не может быть позже 2100")
    private Integer endYear;

    @Size(max = 2000)
    private String description;
}