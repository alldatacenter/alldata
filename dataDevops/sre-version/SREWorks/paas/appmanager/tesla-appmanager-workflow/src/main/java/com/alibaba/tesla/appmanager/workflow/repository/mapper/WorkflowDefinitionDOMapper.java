package com.alibaba.tesla.appmanager.workflow.repository.mapper;

import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowDefinitionDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowDefinitionDOExample;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkflowDefinitionDOMapper {
    long countByExample(WorkflowDefinitionDOExample example);

    int deleteByExample(WorkflowDefinitionDOExample example);

    int insertSelective(WorkflowDefinitionDO record);

    List<WorkflowDefinitionDO> selectByExample(WorkflowDefinitionDOExample example);

    int updateByExampleSelective(@Param("record") WorkflowDefinitionDO record, @Param("example") WorkflowDefinitionDOExample example);
}