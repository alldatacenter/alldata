package com.hw.lineage.server.application.command.task;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @description: CreateTaskCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CreateTaskCmd {

    @NotBlank
    private String taskName;

    private String descr;

    @NotNull
    private Long pluginId;

    @NotNull
    private Long catalogId;

}
