package com.alibaba.tdata.aisp.server.repository.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.condition.TaskQueryCondition;
import com.alibaba.tdata.aisp.server.common.condition.TaskQueryCondition.TaskQueryConditionBuilder;
import com.alibaba.tdata.aisp.server.common.utils.DateFormatUtil;
import com.alibaba.tdata.aisp.server.repository.AnalyseTaskRepository;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDO;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDOExample;
import com.alibaba.tdata.aisp.server.repository.domain.TaskDOExample.Criteria;
import com.alibaba.tdata.aisp.server.repository.domain.TaskTrendDO;
import com.alibaba.tdata.aisp.server.repository.mapper.TaskDOMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @ClassName: AnalyseTaskRepositoryImpl
 * @Author: dyj
 * @DATE: 2021-11-15
 * @Description:
 **/
@Slf4j
@Component
public class AnalyseTaskRepositoryImpl implements AnalyseTaskRepository {
    @Resource
    private TaskDOMapper taskDOMapper;
    /**
     * @param taskDO
     * @return
     */
    @Override
    public int insert(TaskDO taskDO) {
        Date date = new Date();
        taskDO.setGmtCreate(date);
        taskDO.setGmtModified(date);
        return taskDOMapper.insert(taskDO);
    }

    /**
     * @param taskDO
     * @return
     */
    @Override
    public int updateSelectiveById(TaskDO taskDO) {
        updateTime(taskDO);
        return taskDOMapper.updateByPrimaryKeySelective(taskDO);
    }

    /**
     * @param taskUUID
     * @return
     */
    @Override
    public int delete(String taskUUID) {
        return taskDOMapper.deleteByPrimaryKey(taskUUID);
    }

    @Override
    public List<TaskDO> queryByRowBounds(TaskQueryCondition taskQueryCondition, Integer page, Integer pageSize) {
        TaskDOExample example = convert(taskQueryCondition);
        example.setOrderByClause("`gmt_create` DESC");
        RowBounds rowBounds = new RowBounds(page, pageSize);
        return taskDOMapper.selectByExampleWithRowbounds(example, rowBounds);
    }

    @Override
    public long count(TaskQueryCondition condition) {
        TaskDOExample example = convert(condition);
        return taskDOMapper.countByExample(example);
    }

    @Override
    public TaskDO queryById(String taskUUID) {
        return taskDOMapper.selectByPrimaryKey(taskUUID);
    }

    @Override
    public TaskDO queryByIdWithBlobs(String taskUUID) {
        return taskDOMapper.selectByPrimaryKeyWithBlobs(taskUUID);
    }

    @Override
    public List<TaskTrendDO> queryTrend(String sceneCode, String detectorCode,  Date start, Date end) {
        return taskDOMapper.selectTrend(sceneCode, detectorCode,
            DateFormatUtil.simpleFormate(start), DateFormatUtil.simpleFormate(end));
    }

    @Override
    public void cleanResult(Date date) {
        TaskDOExample example = new TaskDOExample();
        example.createCriteria().andGmtCreateLessThan(date);
        taskDOMapper.deleteByExample(example);
    }

    @Override
    public List<TaskDO> queryByCondition(TaskQueryCondition condition) {
        TaskDOExample example = convert(condition);
        return taskDOMapper.selectByExample(example);
    }

    private void updateTime(TaskDO taskDO) {
        taskDO.setGmtModified(new Date());
    }

    private TaskDOExample convert(TaskQueryCondition condition) {
        TaskDOExample example = new TaskDOExample();
        Criteria criteria = example.createCriteria();
        if (!StringUtils.isEmpty(condition.getTaskUuid())){
        criteria.andTaskUuidEqualTo(condition.getTaskUuid());
        }
        if (!StringUtils.isEmpty(condition.getSceneCode())){
            criteria.andSceneCodeEqualTo(condition.getSceneCode());
        }
        if (!StringUtils.isEmpty(condition.getDetectorCode())){
            criteria.andDetectorCodeEqualTo(condition.getDetectorCode());
        }
        if (condition.getTaskType()!=null){
            criteria.andTaskTypeEqualTo(condition.getTaskType().getValue());
        }
        if (condition.getTaskStatus()!=null){
            criteria.andTaskStatusEqualTo(condition.getTaskStatus().getValue());
        }
        if (!StringUtils.isEmpty(condition.getInstanceCode())){
            criteria.andInstanceCodeEqualTo(condition.getInstanceCode());
        }
        if (condition.getStartTime()!=null){
            criteria.andGmtCreateGreaterThan(condition.getStartTime());
        }
        if (condition.getEndTime()!=null){
            criteria.andGmtCreateLessThan(condition.getEndTime());
        }
        return example;
    }
}
