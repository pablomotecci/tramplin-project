package tramplin.dto.resume;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ExperienceResponse {
    private UUID id;
    private String organization;
    private String position;
    private String startDate;
    private String endDate;
    private String description;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}