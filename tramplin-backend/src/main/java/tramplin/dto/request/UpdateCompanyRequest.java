package tramplin.dto.request;

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
public class UpdateCompanyRequest {

    @NotBlank
    private String companyName;

    private String description;

    private String industry;

    @Size(min = 10, max = 12)
    private String inn;

    private String websiteUrl;

    private String city;

    private String address;

    private String phone;

    @Email
    private String email;

    private java.util.List<String> officePhotos;

    private String videoUrl;
}