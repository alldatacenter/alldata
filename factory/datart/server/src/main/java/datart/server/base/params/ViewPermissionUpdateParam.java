package datart.server.base.params;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ViewPermissionUpdateParam extends BaseUpdateParam {

    private String rowPermission;

    private String columnPermission;

}