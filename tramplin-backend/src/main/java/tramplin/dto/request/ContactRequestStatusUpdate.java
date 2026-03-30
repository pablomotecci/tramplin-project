package tramplin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.ContactRequestStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestStatusUpdate {

    @NotNull
    private ContactRequestStatus status;
}