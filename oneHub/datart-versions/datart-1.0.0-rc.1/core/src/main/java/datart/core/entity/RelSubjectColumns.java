package datart.core.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelSubjectColumns extends BaseEntity {
    private String viewId;

    private String subjectId;

    private String subjectType;

    private String columnPermission;
}