package tramplin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {

    private UUID contactRequestId;
    private UUID userId;
    private String displayName;
    private String email;
    private LocalDateTime connectedAt;
}