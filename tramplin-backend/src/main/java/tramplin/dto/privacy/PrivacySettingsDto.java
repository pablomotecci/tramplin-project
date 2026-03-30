package tramplin.dto.privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.Visibility;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettingsDto {

    private Visibility profileVisibility;
    private Visibility resumeVisibility;
    private Visibility applicationsVisibility;
    private Visibility contactsVisibility;
}