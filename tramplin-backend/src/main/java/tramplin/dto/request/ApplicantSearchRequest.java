package tramplin.dto.request;

import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class ApplicantSearchRequest {

    private String query;
    private Set<UUID> tagIds;
    private String university;
    private Integer graduationYearMin;
    private Integer graduationYearMax;
    private Integer page = 0;
    private Integer size = 20;
    private String sort = "name";
}