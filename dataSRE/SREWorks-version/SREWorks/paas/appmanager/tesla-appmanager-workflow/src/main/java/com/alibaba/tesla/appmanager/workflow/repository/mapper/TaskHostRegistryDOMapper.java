package com.alibaba.tesla.appmanager.workflow.repository.mapper;

import com.alibaba.tesla.appmanager.workflow.repository.domain.TaskHostRegistryDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.TaskHostRegistryDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskHostRegistryDOMapper {
    long countByExample(TaskHostRegistryDOExample example);

    int deleteByExample(TaskHostRegistryDOExample example);

    int insertSelective(TaskHostRegistryDO record);

    List<TaskHostRegistryDO> selectByExample(TaskHostRegistryDOExample example);

    int updateByExampleSelective(@Param("record") TaskHostRegistryDO record, @Param("example") TaskHostRegistryDOExample example);
}