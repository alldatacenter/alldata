package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelWidgetWidget extends BaseEntity {
    private String sourceId;

    private String targetId;

    private String config;
}