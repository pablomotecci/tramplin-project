package tramplin.dto.request;

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
public class UpdateCompanyRequest {

    @NotBlank
    private String companyName;

    private String description;

    private String industry;

    @Inn
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