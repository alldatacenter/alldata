package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrgSettings extends BaseEntity {
    private String orgId;

    private String type;

    private String config;
}