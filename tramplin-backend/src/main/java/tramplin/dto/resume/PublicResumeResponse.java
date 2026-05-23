package tramplin.dto.resume;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PublicResumeResponse {
    private List<ExperienceResponse> experiences;
    private List<ProjectResponse> projects;
    private List<EducationResponse> education;
}