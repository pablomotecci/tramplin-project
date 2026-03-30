package tramplin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {
    private long companiesCount;
    private long opportunitiesCount;
    private long internshipsCount;
    private long applicantsCount;
}

