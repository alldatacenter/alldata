package com.alibaba.tesla.appmanager.plugin.service;

import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginDefinitionDO;

/**
 * Plugin 服务接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PluginDefinitionService {
    int create(PluginDefinitionDO pluginDefinition);

}
