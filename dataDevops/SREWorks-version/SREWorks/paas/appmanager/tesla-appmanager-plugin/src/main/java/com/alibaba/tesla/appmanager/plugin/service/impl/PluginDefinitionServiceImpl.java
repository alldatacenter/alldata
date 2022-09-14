package com.alibaba.tesla.appmanager.plugin.service.impl;

import com.alibaba.tesla.appmanager.plugin.repository.PluginDefinitionRepository;
import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginDefinitionDO;
import com.alibaba.tesla.appmanager.plugin.service.PluginDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Plugin 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class PluginDefinitionServiceImpl implements PluginDefinitionService {

    @Autowired
    private PluginDefinitionRepository pluginDefinitionRepository;


    @Override
    public int create(PluginDefinitionDO record) {
        return pluginDefinitionRepository.insert(record);
    }

}
