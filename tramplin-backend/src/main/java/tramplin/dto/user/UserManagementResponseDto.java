package tramplin.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.AccountStatus;
import tramplin.entity.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponseDto {

    private UUID id;
    private String email;
    private String displayName;
    private Role role;
    private AccountStatus status;
    private LocalDateTime createdAt;
}