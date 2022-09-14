package com.alibaba.tesla.appmanager.workflow.repository.mapper;

import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowSnapshotDOExample;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkflowSnapshotDOMapper {
    long countByExample(WorkflowSnapshotDOExample example);

    int deleteByExample(WorkflowSnapshotDOExample example);

    int insertSelective(WorkflowSnapshotDO record);

    List<WorkflowSnapshotDO> selectByExample(WorkflowSnapshotDOExample example);

    int updateByExampleSelective(@Param("record") WorkflowSnapshotDO record, @Param("example") WorkflowSnapshotDOExample example);
}