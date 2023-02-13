package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelWidgetElement extends BaseEntity {
    private String widgetId;

    private String relType;

    private String relId;
}