package tramplin.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.VerificationRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequestResponseDto {

    private UUID id;
    private UUID employerId;
    private String companyName;
    private String inn;
    private String companyDomain;
    private String corporateEmail;
    private VerificationRequestStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
}