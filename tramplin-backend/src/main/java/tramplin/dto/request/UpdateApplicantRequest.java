package tramplin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateApplicantRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String middleName;

    private String university;

    @Min(1)
    @Max(6)

    private Integer course;

    private Integer graduationYear;

    private String bio;

    private String phone;

    private String portfolioUrl;
    private String githubUrl;
    private String skillsSummary;
}