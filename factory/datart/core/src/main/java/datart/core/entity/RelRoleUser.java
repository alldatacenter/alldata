package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelRoleUser extends BaseEntity {
    private String userId;

    private String roleId;
}