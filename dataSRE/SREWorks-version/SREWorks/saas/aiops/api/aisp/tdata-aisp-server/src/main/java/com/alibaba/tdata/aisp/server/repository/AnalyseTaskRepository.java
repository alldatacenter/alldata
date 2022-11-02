package com.alibaba.tdata.aisp.server.repository;

import java.util.Date;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.condition.TaskQueryCondition;
import com.alibaba.tdata.aisp.server.common.condition.TaskQueryCondition.TaskQueryConditionBuilder;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDO;
import com.alibaba.tdata.aisp.server.repository.domain.TaskTrendDO;

/**
 * @InterfaceName:AnalyseTaskRepository
 * @Author:dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
public interface AnalyseTaskRepository {
    /**
     * @param taskDO
     * @return
     */
    public int insert(TaskDO taskDO);

    /**
     * @param taskDO
     * @return
     */
    public int updateSelectiveById(TaskDO taskDO);

    /**
     * @param taskUUID
     * @return
     */
    public int delete(String taskUUID);

    /**
     * @param taskQueryCondition
     * @param page
     * @param pageSize
     * @return
     */
    List<TaskDO> queryByRowBounds(TaskQueryCondition taskQueryCondition, Integer page, Integer pageSize);

    /**
     * @param condition
     * @return
     */
    long count(TaskQueryCondition condition);

    /**
     * @param taskUUID
     * @return
     */
    TaskDO queryById(String taskUUID);

    /**
     * @param taskUUID
     * @return
     */
    TaskDO queryByIdWithBlobs(String taskUUID);

    /**
     * @param sceneCode
     * @param detectorCode
     * @param start
     * @param end
     * @return
     */
    List<TaskTrendDO> queryTrend(String sceneCode, String detectorCode,  Date start, Date end);

    /**
     * @param date
     */
    void cleanResult(Date date);

    /**
     * @param condition
     * @return
     */
    List<TaskDO> queryByCondition(TaskQueryCondition condition);
}
