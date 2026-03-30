package tramplin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileResponse {

    private UUID userId;
    private String companyName;
    private String description;
    private String industry;
    private String inn;
    private String websiteUrl;
    private String logoUrl;
    private String city;
    private String address;
    private String phone;
    private String email;
    private List<String> officePhotos;
    private String videoUrl;
    private String verificationStatus;
}