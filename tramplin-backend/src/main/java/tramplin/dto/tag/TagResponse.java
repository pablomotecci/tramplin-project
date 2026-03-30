package tramplin.dto.tag;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.TagCategory;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagResponse {

    private UUID id;
    private String name;
    private TagCategory category;
    private boolean approved;
    private UUID parentId;
    private String parentName;
}