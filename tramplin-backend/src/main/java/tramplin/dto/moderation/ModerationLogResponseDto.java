package tramplin.dto.moderation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.ModerationAction;
import tramplin.entity.enums.TargetType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationLogResponseDto {

    private UUID id;
    private UUID curatorId;
    private String curatorName;
    private ModerationAction action;
    private TargetType targetType;
    private UUID targetId;
    private String reason;
    private String details;
    private LocalDateTime createdAt;
}