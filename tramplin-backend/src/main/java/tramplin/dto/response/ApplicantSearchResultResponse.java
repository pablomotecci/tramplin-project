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
public class ApplicantSearchResultResponse {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String university;
    private Integer graduationYear;
    private List<String> topTags;
}