package com.hw.lineage.server.domain.query.plugin;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description: PluginCheck
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class PluginCheck {

    @NotNull
    private String pluginName;
}
