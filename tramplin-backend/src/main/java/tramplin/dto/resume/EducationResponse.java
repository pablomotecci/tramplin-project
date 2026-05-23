package tramplin.dto.resume;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EducationResponse {
    private UUID id;
    private String institution;
    private String faculty;
    private String degree;
    private Integer startYear;
    private Integer endYear;
    private String description;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}