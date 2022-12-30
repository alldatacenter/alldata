package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelUserOrganization extends BaseEntity {
    private String orgId;

    private String userId;
}