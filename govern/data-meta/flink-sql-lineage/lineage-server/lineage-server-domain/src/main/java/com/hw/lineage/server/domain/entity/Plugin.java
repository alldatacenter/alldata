package com.hw.lineage.server.domain.entity;

import com.hw.lineage.server.domain.entity.basic.BasicEntity;
import com.hw.lineage.server.domain.repository.basic.Entity;
import com.hw.lineage.server.domain.vo.PluginId;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: Plugin
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
@Accessors(chain = true)
public class Plugin extends BasicEntity implements Entity {

    private PluginId pluginId;

    private String pluginName;

    private String pluginCode;

    private String descr;

    private Boolean defaultPlugin;
}
