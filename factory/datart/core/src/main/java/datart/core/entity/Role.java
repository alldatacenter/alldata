package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity {
    private String orgId;

    private String name;

    private String type;

    private String description;

    private String avatar;
}