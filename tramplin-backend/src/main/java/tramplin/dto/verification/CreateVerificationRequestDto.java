package tramplin.dto.verification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.validation.Inn;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVerificationRequestDto {

    @NotBlank
    @Inn
    private String inn;

    @NotBlank
    private String companyDomain;

    @NotBlank
    @Email
    private String corporateEmail;
}