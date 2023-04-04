package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserSettings extends BaseEntity {
    private String userId;

    private String relType;

    private String relId;

    private String config;
}