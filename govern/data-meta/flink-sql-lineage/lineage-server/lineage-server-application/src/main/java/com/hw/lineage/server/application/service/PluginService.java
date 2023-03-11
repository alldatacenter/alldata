package com.hw.lineage.server.application.service;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.server.application.command.plugin.CreatePluginCmd;
import com.hw.lineage.server.application.command.plugin.UpdatePluginCmd;
import com.hw.lineage.server.application.dto.PluginDTO;
import com.hw.lineage.server.domain.query.plugin.PluginCheck;
import com.hw.lineage.server.domain.query.plugin.PluginQuery;

/**
 * @description: PluginService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface PluginService {

    Long createPlugin(CreatePluginCmd command);

    PluginDTO queryPlugin(Long pluginId);

    Boolean checkPluginExist(PluginCheck pluginCheck);

    PageInfo<PluginDTO> queryPlugins(PluginQuery pluginQuery);

    void deletePlugin(Long pluginId);

    void updatePlugin(UpdatePluginCmd command);

    void defaultPlugin(Long pluginId);
}
