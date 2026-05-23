package tramplin.dto.resume;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ReorderRequest {

    @NotEmpty(message = "Список не может быть пустым")
    @Valid
    private List<ReorderItem> items;

    @Data
    public static class ReorderItem {
        @NotNull(message = "id обязателен")
        private UUID id;

        @NotNull(message = "displayOrder обязателен")
        private Integer displayOrder;
    }
}