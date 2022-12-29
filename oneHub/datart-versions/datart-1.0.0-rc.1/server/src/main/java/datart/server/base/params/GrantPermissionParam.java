package datart.server.base.params;

import datart.security.base.PermissionInfo;
import lombok.Data;

import java.util.List;

@Data
public class GrantPermissionParam {

    private List<PermissionInfo> permissionToDelete;

    private List<PermissionInfo> permissionToUpdate;

    private List<PermissionInfo> permissionToCreate;

}