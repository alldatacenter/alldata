package com.alibaba.tesla.appmanager.plugin.repository.mapper;

import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginTagDO;
import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginTagDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PluginTagDOMapper {
    long countByExample(PluginTagDOExample example);

    int deleteByExample(PluginTagDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PluginTagDO record);

    int insertSelective(PluginTagDO record);

    List<PluginTagDO> selectByExample(PluginTagDOExample example);

    PluginTagDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") PluginTagDO record, @Param("example") PluginTagDOExample example);

    int updateByExample(@Param("record") PluginTagDO record, @Param("example") PluginTagDOExample example);

    int updateByPrimaryKeySelective(PluginTagDO record);

    int updateByPrimaryKey(PluginTagDO record);
}