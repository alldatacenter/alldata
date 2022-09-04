package com.alibaba.tesla.appmanager.plugin.repository.mapper;

import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginResourceDO;
import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginResourceDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PluginResourceDOMapper {
    long countByExample(PluginResourceDOExample example);

    int deleteByExample(PluginResourceDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PluginResourceDO record);

    int insertSelective(PluginResourceDO record);

    List<PluginResourceDO> selectByExample(PluginResourceDOExample example);

    PluginResourceDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") PluginResourceDO record, @Param("example") PluginResourceDOExample example);

    int updateByExample(@Param("record") PluginResourceDO record, @Param("example") PluginResourceDOExample example);

    int updateByPrimaryKeySelective(PluginResourceDO record);

    int updateByPrimaryKey(PluginResourceDO record);
}