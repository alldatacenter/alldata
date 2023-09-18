package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelRoleResource extends BaseEntity {
    private String roleId;

    private String resourceId;

    private String resourceType;

    private String orgId;
}