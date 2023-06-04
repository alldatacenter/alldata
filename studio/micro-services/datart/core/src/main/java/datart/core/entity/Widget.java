package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Widget extends BaseEntity {
    private String dashboardId;

    private String config;

    private String parentId;
}