package com.hw.lineage.server.application.command.user;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description: UpdateUserCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UpdateUserCmd {

    @ApiModelProperty(hidden = true)
    private Long userId;

    private String username;

    private String password;

    private Boolean locked;
}
