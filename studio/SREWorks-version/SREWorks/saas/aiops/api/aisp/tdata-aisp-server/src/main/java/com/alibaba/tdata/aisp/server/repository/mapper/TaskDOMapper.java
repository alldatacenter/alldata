package com.alibaba.tdata.aisp.server.repository.mapper;

import com.alibaba.tdata.aisp.server.repository.domain.TaskDO;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDOExample;
import com.alibaba.tdata.aisp.server.repository.domain.TaskTrendDO;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface TaskDOMapper {
    long countByExample(TaskDOExample example);

    int deleteByExample(TaskDOExample example);

    int deleteByPrimaryKey(String taskUuid);

    int insert(TaskDO record);

    int insertSelective(TaskDO record);

    List<TaskDO> selectByExampleWithBLOBsWithRowbounds(TaskDOExample example, RowBounds rowBounds);

    List<TaskDO> selectByExampleWithBLOBs(TaskDOExample example);

    List<TaskDO> selectByExampleWithRowbounds(TaskDOExample example, RowBounds rowBounds);

    List<TaskDO> selectByExample(TaskDOExample example);

    TaskDO selectByPrimaryKey(String taskUuid);

    TaskDO selectByPrimaryKeyWithBlobs(String taskUuid);

    int updateByExampleSelective(@Param("record") TaskDO record, @Param("example") TaskDOExample example);

    int updateByExampleWithBLOBs(@Param("record") TaskDO record, @Param("example") TaskDOExample example);

    int updateByExample(@Param("record") TaskDO record, @Param("example") TaskDOExample example);

    int updateByPrimaryKeySelective(TaskDO record);

    int updateByPrimaryKeyWithBLOBs(TaskDO record);

    int updateByPrimaryKey(TaskDO record);

    int batchInsert(@Param("list") List<TaskDO> list);

    int batchInsertSelective(@Param("list") List<TaskDO> list, @Param("selective") TaskDO.Column ... selective);

    int upsert(TaskDO record);

    int upsertSelective(TaskDO record);

    int upsertWithBLOBs(TaskDO record);

    List<TaskTrendDO> selectTrend(@Param("sceneCode")String sceneCode, @Param("detectorCode")String detectorCode,
        @Param("start")String start, @Param("end")String end);
}