package com.hw.lineage.server.application.command.plugin;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: UpdatePluginCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UpdatePluginCmd {

    @ApiModelProperty(hidden = true)
    private Long pluginId;

    private String pluginName;

    private String descr;
}
