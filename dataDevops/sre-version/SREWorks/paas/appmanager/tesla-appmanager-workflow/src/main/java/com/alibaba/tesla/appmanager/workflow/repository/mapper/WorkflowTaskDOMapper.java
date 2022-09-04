package com.alibaba.tesla.appmanager.workflow.repository.mapper;

import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WorkflowTaskDOMapper {
    long countByExample(WorkflowTaskDOExample example);

    int deleteByExample(WorkflowTaskDOExample example);

    int insertSelective(WorkflowTaskDO record);

    List<WorkflowTaskDO> selectByExampleWithBLOBs(WorkflowTaskDOExample example);

    List<WorkflowTaskDO> selectByExample(WorkflowTaskDOExample example);

    int updateByExampleSelective(@Param("record") WorkflowTaskDO record, @Param("example") WorkflowTaskDOExample example);

    int updateByPrimaryKeySelective(WorkflowTaskDO record);

    /**
     * 获取指定 workflowInstance 中指定 workflowTask 的下一个 PENDING 待运行任务
     *
     * @param workflowInstanceId Workflow Instance ID
     * @param workflowTaskId     Workflow Task ID
     * @return 待运行 Workflow 任务
     */
    WorkflowTaskDO nextPendingTask(
            @Param("workflowInstanceId") Long workflowInstanceId,
            @Param("workflowTaskId") Long workflowTaskId);

    /**
     * 列出当前所有正在运行中的远程 workflow task
     *
     * @return List or WorkflowTaskDO
     */
    List<WorkflowTaskDO> listRunningRemoteTask();
}