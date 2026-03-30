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
public class FavoriteResponse {

    private UUID id;
    private UUID opportunityId;
    private String opportunityTitle;
    private String companyName;
    private LocalDateTime createdAt;
}