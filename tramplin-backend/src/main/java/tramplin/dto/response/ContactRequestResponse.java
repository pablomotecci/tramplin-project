package tramplin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.ContactRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestResponse {

    private UUID id;
    private UUID senderId;
    private String senderDisplayName;
    private String senderEmail;
    private ContactRequestStatus status;
    private LocalDateTime createdAt;
}