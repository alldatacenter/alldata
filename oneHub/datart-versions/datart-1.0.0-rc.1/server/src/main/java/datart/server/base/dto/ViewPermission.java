package datart.server.base.dto;

import datart.security.base.SubjectType;
import lombok.Data;

import java.util.List;

@Data
public class ViewPermission {

    private String id;

    private String orgId;

    private SubjectType subjectType;

    private String subjectId;

    private String subjectName;

    private String viewId;

    private String viewName;

    private List<VariableValue> variableValues;

    private String columnPermission;

}
