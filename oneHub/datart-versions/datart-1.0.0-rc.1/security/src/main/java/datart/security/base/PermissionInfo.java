package datart.security.base;

import lombok.Data;

@Data
public class PermissionInfo {

    private String id;

    private String orgId;

    private SubjectType subjectType;

    private String subjectId;

    private ResourceType resourceType;

    private String resourceId;

    private int permission;

}
