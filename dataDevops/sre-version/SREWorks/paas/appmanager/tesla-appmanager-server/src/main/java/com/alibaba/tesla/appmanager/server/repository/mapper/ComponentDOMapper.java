package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ComponentDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ComponentDOMapper {
    long countByExample(ComponentDOExample example);

    int deleteByExample(ComponentDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(ComponentDO record);

    int insertSelective(ComponentDO record);

    List<ComponentDO> selectByExampleWithBLOBs(ComponentDOExample example);

    List<ComponentDO> selectByExample(ComponentDOExample example);

    ComponentDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ComponentDO record, @Param("example") ComponentDOExample example);

    int updateByPrimaryKeySelective(ComponentDO record);
}