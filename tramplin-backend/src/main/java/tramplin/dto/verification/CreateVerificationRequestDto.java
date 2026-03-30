package tramplin.dto.verification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVerificationRequestDto {

    @NotBlank
    @Size(min = 10, max = 12)
    private String inn;

    @NotBlank
    private String companyDomain;

    @NotBlank
    @Email
    private String corporateEmail;
}