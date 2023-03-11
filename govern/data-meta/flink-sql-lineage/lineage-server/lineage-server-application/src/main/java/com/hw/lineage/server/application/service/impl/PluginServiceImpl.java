package com.hw.lineage.server.application.service.impl;

import com.github.pagehelper.PageInfo;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.application.assembler.DtoAssembler;
import com.hw.lineage.server.application.command.plugin.CreatePluginCmd;
import com.hw.lineage.server.application.command.plugin.UpdatePluginCmd;
import com.hw.lineage.server.application.dto.PluginDTO;
import com.hw.lineage.server.application.service.PluginService;
import com.hw.lineage.server.domain.entity.Plugin;
import com.hw.lineage.server.domain.query.plugin.PluginCheck;
import com.hw.lineage.server.domain.query.plugin.PluginQuery;
import com.hw.lineage.server.domain.repository.PluginRepository;
import com.hw.lineage.server.domain.vo.PluginId;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @description: PluginServiceImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class PluginServiceImpl implements PluginService {

    @Resource
    private PluginRepository repository;

    @Resource
    private DtoAssembler assembler;

    @Override
    public Long createPlugin(CreatePluginCmd command) {
        Plugin plugin = new Plugin()
                .setPluginName(command.getPluginName())
                .setDescr(command.getDescr())
                .setDefaultPlugin(command.getDefaultPlugin());

        plugin.setCreateTime(System.currentTimeMillis())
                .setModifyTime(System.currentTimeMillis())
                .setInvalid(false);

        plugin = repository.save(plugin);
        return plugin.getPluginId().getValue();
    }

    @Override
    public PluginDTO queryPlugin(Long pluginId) {
        Plugin plugin = repository.find(new PluginId(pluginId));
        return assembler.fromPlugin(plugin);
    }

    @Override
    public Boolean checkPluginExist(PluginCheck pluginCheck) {
        return repository.check(pluginCheck.getPluginName());
    }

    @Override
    public PageInfo<PluginDTO> queryPlugins(PluginQuery pluginQuery) {
        PageInfo<Plugin> pageInfo = repository.findAll(pluginQuery);
        return PageUtils.convertPage(pageInfo, assembler::fromPlugin);
    }

    @Override
    public void deletePlugin(Long pluginId) {
        repository.remove(new PluginId(pluginId));
    }

    @Override
    public void updatePlugin(UpdatePluginCmd command) {
        Plugin plugin = new Plugin()
                .setPluginId(new PluginId(command.getPluginId()))
                .setPluginName(command.getPluginName())
                .setDescr(command.getDescr());

        plugin.setModifyTime(System.currentTimeMillis());
        repository.save(plugin);
    }

    @Override
    public void defaultPlugin(Long pluginId) {
        repository.setDefault(new PluginId(pluginId));
    }
}
