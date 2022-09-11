/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.core.impl;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.db.CommandEntity;
import org.apache.inlong.common.enums.ComponentTypeEnum;
import org.apache.inlong.common.enums.PullJobTypeEnum;
import org.apache.inlong.common.pojo.agent.CmdConfig;
import org.apache.inlong.common.pojo.agent.DataConfig;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.source.file.FileSourceDTO;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamConfigLogRequest;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.DataSourceCmdConfigEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.service.core.AgentService;
import org.apache.inlong.manager.service.core.StreamConfigLogService;
import org.apache.inlong.manager.service.source.SourceSnapshotOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Agent service layer implementation
 */
@Service
public class AgentServiceImpl implements AgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentServiceImpl.class);
    private static final int UNISSUED_STATUS = 2;
    private static final int ISSUED_STATUS = 3;
    private static final int MODULUS_100 = 100;
    private static final int TASK_FETCH_SIZE = 2;

    @Autowired
    private StreamSourceEntityMapper sourceMapper;
    @Autowired
    private SourceSnapshotOperation snapshotOperation;
    @Autowired
    private DataSourceCmdConfigEntityMapper sourceCmdConfigMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private StreamConfigLogService streamConfigLogService;

    @Override
    public Boolean reportSnapshot(TaskSnapshotRequest request) {
        return snapshotOperation.snapshot(request);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW)
    public void report(TaskRequest request) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("begin to get agent task: {}", request);
        }
        if (request == null || StringUtils.isBlank(request.getAgentIp())) {
            throw new BusinessException("agent request or agent ip was empty, just return");
        }

        // Update task status, other tasks with status 20x will change to 30x in next request
        if (CollectionUtils.isEmpty(request.getCommandInfo())) {
            LOGGER.warn("task result was empty, just return");
            return;
        }
        for (CommandEntity command : request.getCommandInfo()) {
            updateTaskStatus(command);
        }
    }

    /**
     * Update task status by command.
     *
     * @param command command info.
     */
    private void updateTaskStatus(CommandEntity command) {
        Integer taskId = command.getTaskId();
        StreamSourceEntity current = sourceMapper.selectForAgentTask(taskId);
        if (current == null) {
            LOGGER.warn("stream source not found by id={}, just return", taskId);
            return;
        }

        if (!Objects.equals(command.getVersion(), current.getVersion())) {
            LOGGER.warn("task result version [{}] not equals to current [{}] for id [{}], skip update",
                    command.getVersion(), current.getVersion(), taskId);
            return;
        }

        int result = command.getCommandResult();
        int previousStatus = current.getStatus();
        int nextStatus = SourceStatus.SOURCE_NORMAL.getCode();

        if (Constants.RESULT_FAIL == result) {
            logFailedStreamSource(current);
            nextStatus = SourceStatus.SOURCE_FAILED.getCode();
        } else if (previousStatus / MODULUS_100 == ISSUED_STATUS) {
            // Change the status from 30x to normal / disable / frozen
            if (SourceStatus.TEMP_TO_NORMAL.contains(previousStatus)) {
                nextStatus = SourceStatus.SOURCE_NORMAL.getCode();
            } else if (SourceStatus.BEEN_ISSUED_DELETE.getCode() == previousStatus) {
                nextStatus = SourceStatus.SOURCE_DISABLE.getCode();
            } else if (SourceStatus.BEEN_ISSUED_FROZEN.getCode() == previousStatus) {
                nextStatus = SourceStatus.SOURCE_FROZEN.getCode();
            }
        }

        if (nextStatus != previousStatus) {
            sourceMapper.updateStatus(taskId, nextStatus, false);
            LOGGER.info("task result=[{}], update source status to [{}] for id [{}]", result, nextStatus, taskId);
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.READ_COMMITTED,
            propagation = Propagation.REQUIRES_NEW)
    public TaskResult getTaskResult(TaskRequest request) {
        if (request == null || StringUtils.isBlank(request.getAgentIp())) {
            throw new BusinessException("agent request or agent ip was empty, just return");
        }

        final String agentIp = request.getAgentIp();
        final String uuid = request.getUuid();
        // Query the tasks that needed to add or active - without agentIp and uuid
        List<Integer> needAddStatusList;
        if (PullJobTypeEnum.NEVER != PullJobTypeEnum.getPullJobType(request.getPullJobType())) {
            needAddStatusList = Arrays.asList(SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                    SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        } else {
            LOGGER.warn("agent pull job type is [NEVER], just pull to be active tasks");
            needAddStatusList = Collections.singletonList(SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        }
        List<String> sourceTypes = Lists.newArrayList(SourceType.BINLOG.getType(), SourceType.KAFKA.getType(),
                SourceType.SQL.getType());
        List<StreamSourceEntity> entityList = sourceMapper.selectByStatusAndType(needAddStatusList, sourceTypes,
                TASK_FETCH_SIZE);

        // Query other tasks by agentIp and uuid - not included status with TO_BE_ISSUED_ADD and TO_BE_ISSUED_ACTIVE
        List<Integer> statusList = Arrays.asList(SourceStatus.TO_BE_ISSUED_DELETE.getCode(),
                SourceStatus.TO_BE_ISSUED_RETRY.getCode(), SourceStatus.TO_BE_ISSUED_BACKTRACK.getCode(),
                SourceStatus.TO_BE_ISSUED_FROZEN.getCode(), SourceStatus.TO_BE_ISSUED_CHECK.getCode(),
                SourceStatus.TO_BE_ISSUED_REDO_METRIC.getCode(), SourceStatus.TO_BE_ISSUED_MAKEUP.getCode());
        List<StreamSourceEntity> needIssuedList = sourceMapper.selectByStatusAndIp(statusList, agentIp, uuid);
        entityList.addAll(needIssuedList);

        List<StreamSourceEntity> fileEntityList = sourceMapper.selectByStatusAndType(needAddStatusList,
                Lists.newArrayList(SourceType.FILE.getType()), TASK_FETCH_SIZE * 2);
        for (StreamSourceEntity fileEntity : fileEntityList) {
            FileSourceDTO fileSourceDTO = FileSourceDTO.getFromJson(fileEntity.getExtParams());
            if (agentIp.equals(fileSourceDTO.getIp())) {
                entityList.add(fileEntity);
            }
        }

        List<DataConfig> dataConfigs = Lists.newArrayList();
        for (StreamSourceEntity entity : entityList) {
            // Change 20x to 30x
            int id = entity.getId();
            entity = sourceMapper.selectForAgentTask(id);
            int status = entity.getStatus();
            int op = status % MODULUS_100;
            if (status / MODULUS_100 == UNISSUED_STATUS) {
                int nextStatus = ISSUED_STATUS * MODULUS_100 + op;
                sourceMapper.updateStatus(id, nextStatus, false);
                LOGGER.info("update stream source status to [{}] for id [{}] ", nextStatus, id);
            } else {
                LOGGER.info("skip task status not in 20x, id={}", id);
                continue;
            }

            DataConfig dataConfig = getDataConfig(entity, op);
            dataConfigs.add(dataConfig);
        }
        // Query pending special commands
        List<CmdConfig> cmdConfigs = getAgentCmdConfigs(request);

        // Update agentIp and uuid for the added and active tasks
        for (StreamSourceEntity entity : entityList) {
            if (StringUtils.isEmpty(entity.getAgentIp())) {
                sourceMapper.updateIpAndUuid(entity.getId(), agentIp, uuid, false);
                LOGGER.info("update stream source ip to [{}], uuid to [{}] for id [{}]", agentIp, uuid, entity.getId());
            }
        }

        return TaskResult.builder().dataConfigs(dataConfigs).cmdConfigs(cmdConfigs).build();
    }

    /**
     * If status of source is failed, record.
     *
     * @param entity
     */
    private void logFailedStreamSource(StreamSourceEntity entity) {
        InlongStreamConfigLogRequest request = new InlongStreamConfigLogRequest();
        request.setInlongGroupId(entity.getInlongGroupId());
        request.setInlongStreamId(entity.getInlongStreamId());
        request.setComponentName(ComponentTypeEnum.Agent.getName());
        request.setIp(entity.getAgentIp());
        request.setConfigName("DataSource:" + entity.getSourceName());
        request.setLogType(1);
        request.setLogInfo(String.format("StreamSource=%s init failed, please check!", entity));
        request.setReportTime(new Date().getTime());
        streamConfigLogService.reportConfigLog(request);
    }

    /**
     * Get the DataConfig from the stream source entity.
     *
     * @param entity stream source entity.
     * @param op operation code for add, delete, etc.
     * @return data config.
     */
    private DataConfig getDataConfig(StreamSourceEntity entity, int op) {
        DataConfig dataConfig = new DataConfig();
        dataConfig.setIp(entity.getAgentIp());
        dataConfig.setUuid(entity.getUuid());
        dataConfig.setOp(String.valueOf(op));
        dataConfig.setTaskId(entity.getId());
        dataConfig.setTaskType(getTaskType(entity));
        dataConfig.setTaskName(entity.getSourceName());
        dataConfig.setSnapshot(entity.getSnapshot());
        dataConfig.setExtParams(entity.getExtParams());
        dataConfig.setVersion(entity.getVersion());

        String groupId = entity.getInlongGroupId();
        String streamId = entity.getInlongStreamId();
        dataConfig.setInlongGroupId(groupId);
        dataConfig.setInlongStreamId(streamId);
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        if (streamEntity != null) {
            dataConfig.setSyncSend(streamEntity.getSyncSend());
        } else {
            dataConfig.setSyncSend(0);
            LOGGER.warn("set syncSend=[0] as the stream not exists for groupId={}, streamId={}", groupId, streamId);
        }
        return dataConfig;
    }

    /**
     * Get the Task type from the stream source entity.
     *
     * @param sourceEntity stream source info.
     * @return task type
     */
    private int getTaskType(StreamSourceEntity sourceEntity) {
        SourceType sourceType = SourceType.forType(sourceEntity.getSourceType());
        return sourceType.getTaskType().getType();
    }

    /**
     * Get the agent command config by the agent ip.
     *
     * @param taskRequest task request info.
     * @return agent command config list.
     */
    private List<CmdConfig> getAgentCmdConfigs(TaskRequest taskRequest) {
        return sourceCmdConfigMapper.queryCmdByAgentIp(taskRequest.getAgentIp()).stream().map(cmd -> {
            CmdConfig cmdConfig = new CmdConfig();
            cmdConfig.setDataTime(cmd.getSpecifiedDataTime());
            cmdConfig.setOp(cmd.getCmdType());
            cmdConfig.setId(cmd.getId());
            cmdConfig.setTaskId(cmd.getTaskId());
            return cmdConfig;
        }).collect(Collectors.toList());
    }

}