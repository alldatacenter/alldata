package com.hw.lineage.server.application.command.task;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @description: UpdateTaskCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class UpdateTaskCmd {

    @ApiModelProperty(hidden = true)
    private Long taskId;

    private String taskName;

    private String descr;

    private Long pluginId;

    private Long catalogId;

    private String source;

}
