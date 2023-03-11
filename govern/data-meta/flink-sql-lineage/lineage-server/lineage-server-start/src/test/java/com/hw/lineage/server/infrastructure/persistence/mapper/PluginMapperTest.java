package com.hw.lineage.server.infrastructure.persistence.mapper;

import com.hw.lineage.common.exception.LineageException;
import com.hw.lineage.server.AbstractSpringBootTest;
import com.hw.lineage.server.infrastructure.persistence.dos.PluginDO;
import org.junit.Test;

import javax.annotation.Resource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @description: PluginMapperTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class PluginMapperTest extends AbstractSpringBootTest {

    @Resource
    private PluginMapper pluginMapper;

    @Test
    public void testSelectByPrimaryKey() {
        Long pluginId = 1L;

        PluginDO pluginDO=pluginMapper.selectByPrimaryKey(pluginId)
                .orElseThrow(() -> new LineageException(String.format("pluginId [%s] is not existed", pluginId)));

        assertThat(pluginDO).isNotNull();
        assertThat(pluginDO.getPluginId()).isEqualTo(1L);
        assertThat(pluginDO.getPluginCode()).isEqualTo("flink1.16.x");
        assertThat(pluginDO.getDescr()).isEqualTo("Field lineage plugin for flink1.16");
    }
}
