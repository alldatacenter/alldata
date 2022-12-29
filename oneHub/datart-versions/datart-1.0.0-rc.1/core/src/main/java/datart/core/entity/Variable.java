package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Variable extends BaseEntity {
    private String orgId;

    private String viewId;

    private String sourceId;

    private String name;

    private String type;

    private String valueType;

    private String format;

    private Boolean encrypt;

    private String label;

    private String defaultValue;

    private Boolean expression;
}