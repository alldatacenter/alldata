package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelVariableSubject extends BaseEntity {
    private String variableId;

    private String subjectId;

    private String subjectType;

    private String value;

    private Boolean useDefaultValue;
}