package com.hw.lineage.server.infrastructure.repository.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.hw.lineage.common.exception.LineageException;
import com.hw.lineage.common.util.PageUtils;
import com.hw.lineage.server.domain.entity.Plugin;
import com.hw.lineage.server.domain.query.plugin.PluginQuery;
import com.hw.lineage.server.domain.repository.PluginRepository;
import com.hw.lineage.server.domain.vo.PluginId;
import com.hw.lineage.server.infrastructure.persistence.converter.DataConverter;
import com.hw.lineage.server.infrastructure.persistence.dos.PluginDO;
import com.hw.lineage.server.infrastructure.persistence.mapper.PluginMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

import static com.hw.lineage.server.infrastructure.persistence.mapper.PluginDynamicSqlSupport.plugin;
import static com.hw.lineage.server.infrastructure.persistence.mapper.PluginDynamicSqlSupport.pluginName;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isLike;


/**
 * @description: PluginRepositoryImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Repository
public class PluginRepositoryImpl extends AbstractBasicRepository implements PluginRepository {

    @Resource
    private PluginMapper pluginMapper;

    @Resource
    private DataConverter converter;

    @Override
    public Plugin find(PluginId pluginId) {
        PluginDO pluginDO = pluginMapper.selectByPrimaryKey(pluginId.getValue())
                .orElseThrow(() -> new LineageException(String.format("pluginId [%s] is not existed", pluginId.getValue())));
        return converter.toPlugin(pluginDO);
    }

    @Override
    public boolean check(String name) {
        return !pluginMapper.select(completer -> completer.where(pluginName, isEqualTo(name))).isEmpty();
    }

    @Override
    public Plugin save(Plugin plugin) {
        PluginDO pluginDO = converter.fromPlugin(plugin);
        if (pluginDO.getPluginId() == null) {
            pluginMapper.insertSelective(pluginDO);
        } else {
            pluginMapper.updateByPrimaryKeySelective(pluginDO);
        }
        return converter.toPlugin(pluginDO);
    }

    @Override
    public void remove(PluginId pluginId) {
        pluginMapper.deleteByPrimaryKey(pluginId.getValue());
    }

    @Override
    public PageInfo<Plugin> findAll(PluginQuery pluginQuery) {
        try (Page<PluginDO> page = PageMethod.startPage(pluginQuery.getPageNum(), pluginQuery.getPageSize())) {
            PageInfo<PluginDO> pageInfo = page.doSelectPageInfo(() ->
                    pluginMapper.select(completer ->
                            completer.where(pluginName, isLike(buildLikeValue(pluginQuery.getPluginName())))
                                    .orderBy(buildSortSpecification(pluginQuery))
                    )
            );
            return PageUtils.convertPage(pageInfo, converter::toPlugin);
        }
    }

    @Override
    public void setDefault(PluginId pluginId) {
        pluginMapper.update(completer ->
                completer.set(plugin.defaultPlugin).equalTo(FALSE).where(plugin.defaultPlugin, isEqualTo(TRUE))
        );
        pluginMapper.update(completer ->
                completer.set(plugin.defaultPlugin).equalTo(TRUE).where(plugin.pluginId, isEqualTo(pluginId.getValue()))
        );
    }
}
