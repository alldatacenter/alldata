package com.hw.lineage.server.application.command.permission;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: UpdatePermissionCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UpdatePermissionCmd {

    @ApiModelProperty(hidden = true)
    private Long permissionId;

    private String permissionGroup;

    private String permissionName;

    private String permissionCode;
}
