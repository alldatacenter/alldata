package datart.server.base.params;

import datart.server.base.dto.ViewPermission;
import lombok.Data;

import java.util.List;

@Data
public class ViewPermissionParam {

    private List<ViewPermission> permissionToCreate;

    private List<ViewPermission> permissionToUpdate;

    private List<String> permissionToDelete;

}