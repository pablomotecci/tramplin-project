package tramplin.dto.tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tramplin.entity.enums.TagCategory;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagTreeResponse {

    private UUID id;
    private String name;
    private TagCategory category;
    private List<TagTreeResponse> children;
}