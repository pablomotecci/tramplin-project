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
public class ApplicantProfileResponse {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String university;
    private Integer course;
    private Integer graduationYear;
    private String bio;
    private String avatarUrl;
    private String phone;
    private String portfolioUrl;
    private String githubUrl;
    private String skillsSummary;
    private List<String> tags;
}