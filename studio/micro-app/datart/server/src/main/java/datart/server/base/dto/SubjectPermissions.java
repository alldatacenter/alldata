package datart.server.base.dto;

import datart.security.base.PermissionInfo;
import datart.security.base.SubjectType;
import lombok.Data;

import java.util.List;

@Data
public class SubjectPermissions {

    private String orgId;

    private String subjectId;

    private SubjectType subjectType;

    private boolean orgOwner;

    private List<PermissionInfo> permissionInfos;

}
