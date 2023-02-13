package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SourceSchemas extends BaseEntity {
    private String sourceId;

    private String schemas;
}