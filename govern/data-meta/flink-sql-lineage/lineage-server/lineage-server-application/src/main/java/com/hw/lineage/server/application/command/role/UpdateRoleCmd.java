package com.hw.lineage.server.application.command.role;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: UpdateRoleCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UpdateRoleCmd {

    @ApiModelProperty(hidden = true)
    private Long roleId;

    private String roleName;
}
