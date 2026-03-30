package tramplin.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email слишком длинный")
    private String email;

    @NotBlank(message = "Отображаемое имя обязательно")
    @Size(min = 2, max = 255, message = "Имя должно быть от 2 до 255 символов")
    private String displayName;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 8, max = 100, message = "Пароль должен быть от 8 до 100 символов")
    private String password;

    @NotBlank(message = "Роль обязательна")
    @Pattern(regexp = "APPLICANT|EMPLOYER",
             message = "Роль должна быть APPLICANT или EMPLOYER")
    private String role;
}
