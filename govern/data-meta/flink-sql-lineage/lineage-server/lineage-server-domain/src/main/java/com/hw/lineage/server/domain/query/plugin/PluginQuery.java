package com.hw.lineage.server.domain.query.plugin;

import com.hw.lineage.server.domain.query.PageOrderCriteria;
import lombok.Data;

/**
 * @description: PluginQuery
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class PluginQuery extends PageOrderCriteria {

    private String pluginName;

    private Boolean defaultPlugin;
}