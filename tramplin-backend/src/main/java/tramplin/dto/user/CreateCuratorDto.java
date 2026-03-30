package tramplin.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCuratorDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String displayName;

    @NotBlank
    @Size(min = 8)
    private String password;
}