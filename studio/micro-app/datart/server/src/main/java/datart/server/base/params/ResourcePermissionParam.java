package datart.server.base.params;


import datart.security.base.ResourceType;
import lombok.Data;

@Data
public class ResourcePermissionParam {

    private String resourceId;

    private ResourceType resourceType;

    private int permission;

    private String orgId;

}
