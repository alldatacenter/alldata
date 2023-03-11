package com.hw.lineage.server.application.command.plugin;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @description: CreatePluginCmd
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class CreatePluginCmd {

    @NotBlank
    private String pluginName;

    private String descr;

    private Boolean defaultPlugin;
}
