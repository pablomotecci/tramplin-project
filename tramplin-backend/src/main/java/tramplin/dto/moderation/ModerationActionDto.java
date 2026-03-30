package tramplin.dto.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationActionDto {

    private String reason;
    private String details;
}
