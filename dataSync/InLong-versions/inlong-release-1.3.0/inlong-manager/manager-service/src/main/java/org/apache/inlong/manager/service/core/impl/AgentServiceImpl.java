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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.db.CommandEntity;
import org.apache.inlong.common.enums.PullJobTypeEnum;
import org.apache.inlong.common.enums.TaskTypeEnum;
import org.apache.inlong.common.pojo.agent.CmdConfig;
import org.apache.inlong.common.pojo.agent.DataConfig;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.DataSourceCmdConfigEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.pojo.source.file.FileSourceDTO;
import org.apache.inlong.manager.service.core.AgentService;
import org.apache.inlong.manager.service.source.SourceSnapshotOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
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
    private SourceSnapshotOperator snapshotOperator;
    @Autowired
    private DataSourceCmdConfigEntityMapper sourceCmdConfigMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;

    @Override
    public Boolean reportSnapshot(TaskSnapshotRequest request) {
        return snapshotOperator.snapshot(request);
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
            LOGGER.warn("task failed for id =[{}]", taskId);
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

        List<DataConfig> tasks = Lists.newArrayList();
        // Query the tasks that needed to add or active - without agentIp and uuid
        List<DataConfig> nonFileTasks = fetchNonFileTasks(request);
        tasks.addAll(nonFileTasks);
        // Query file collecting tasks
        List<DataConfig> fileTasks = fetchFileTasks(request);
        tasks.addAll(fileTasks);
        // Query other tasks by agentIp and uuid - not included status with TO_BE_ISSUED_ADD and TO_BE_ISSUED_ACTIVE
        List<DataConfig> needIssuedTasks = fetchIssuedTasks(request);
        tasks.addAll(needIssuedTasks);

        // Query pending special commands
        List<CmdConfig> cmdConfigs = getAgentCmdConfigs(request);
        return TaskResult.builder().dataConfigs(tasks).cmdConfigs(cmdConfigs).build();
    }

    private List<DataConfig> fetchNonFileTasks(TaskRequest taskRequest) {
        List<Integer> needAddStatusList;
        if (PullJobTypeEnum.NEVER == PullJobTypeEnum.getPullJobType(taskRequest.getPullJobType())) {
            LOGGER.warn("agent pull job type is [NEVER], just pull to be active tasks");
            needAddStatusList = Collections.singletonList(SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        } else {
            needAddStatusList = Arrays.asList(SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                    SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        }
        List<String> sourceTypes = Lists.newArrayList(SourceType.MYSQL_SQL, SourceType.KAFKA,
                SourceType.MYSQL_BINLOG);
        List<StreamSourceEntity> sourceEntities = sourceMapper.selectByStatusAndType(needAddStatusList, sourceTypes,
                TASK_FETCH_SIZE);
        List<DataConfig> nonFileTasks = Lists.newArrayList();
        for (StreamSourceEntity sourceEntity : sourceEntities) {
            int op = getOp(sourceEntity.getStatus());
            int nextStatus = getNextStatus(sourceEntity.getStatus());
            sourceEntity.setStatus(nextStatus);
            sourceEntity.setAgentIp(taskRequest.getAgentIp());
            sourceEntity.setUuid(taskRequest.getUuid());
            if (sourceMapper.updateByPrimaryKeySelective(sourceEntity) == 1) {
                sourceEntity.setVersion(sourceEntity.getVersion() + 1);
                nonFileTasks.add(getDataConfig(sourceEntity, op));
            }
        }
        return nonFileTasks;
    }

    private List<DataConfig> fetchFileTasks(TaskRequest taskRequest) {
        List<Integer> needAddStatusList;
        if (PullJobTypeEnum.NEVER == PullJobTypeEnum.getPullJobType(taskRequest.getPullJobType())) {
            LOGGER.warn("agent pull job type is [NEVER], just pull to be active tasks");
            needAddStatusList = Collections.singletonList(SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        } else {
            needAddStatusList = Arrays.asList(SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                    SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        }
        final String agentIp = taskRequest.getAgentIp();
        final String agentClusterName = taskRequest.getClusterName();
        final String uuid = taskRequest.getUuid();
        Preconditions.checkTrue(StringUtils.isNotBlank(agentIp) || StringUtils.isNotBlank(agentClusterName),
                "both agent ip and cluster name are blank when fetching file task");
        List<StreamSourceEntity> sourceEntities = sourceMapper.selectByAgentIpOrCluster(needAddStatusList,
                Lists.newArrayList(SourceType.FILE), agentIp, agentClusterName);
        List<DataConfig> fileTasks = Lists.newArrayList();
        for (StreamSourceEntity sourceEntity : sourceEntities) {
            FileSourceDTO fileSourceDTO = FileSourceDTO.getFromJson(sourceEntity.getExtParams());
            final String destIp = sourceEntity.getAgentIp();
            final String destClusterName = sourceEntity.getInlongClusterName();

            // Use ip directly if it is not empty
            if (StringUtils.isNotBlank(destIp)) {
                if (destIp.equals(agentIp)) {
                    int op = getOp(sourceEntity.getStatus());
                    int nextStatus = getNextStatus(sourceEntity.getStatus());
                    sourceEntity.setUuid(uuid);
                    sourceEntity.setStatus(nextStatus);
                    if (sourceMapper.updateByPrimaryKeySelective(sourceEntity) == 1) {
                        sourceEntity.setVersion(sourceEntity.getVersion() + 1);
                        fileTasks.add(getDataConfig(sourceEntity, op));
                    }
                }
                continue;
            }

            // Cluster name is not blank, split subtask if necessary
            // The template task's id is assigned to the subtask's template id field
            if (StringUtils.isNotBlank(destClusterName) && destClusterName.equals(agentClusterName)
                    && Objects.isNull(sourceEntity.getTemplateId())) {

                // Is the task already fetched by this agent ?
                List<StreamSourceEntity> subSources = sourceMapper.selectByTemplateId(sourceEntity.getId());
                if (subSources.stream().anyMatch(subSource -> subSource.getAgentIp().equals(agentIp))) {
                    continue;
                }

                // If not, clone a sub task for the new agent
                // Note that a new source name with random suffix is generated to adhere to the unique constraint
                StreamSourceEntity fileEntity = CommonBeanUtils.copyProperties(sourceEntity, StreamSourceEntity::new);
                FileSourceDTO childFileSourceDTO = CommonBeanUtils.copyProperties(fileSourceDTO, FileSourceDTO::new);
                fileEntity.setExtParams(JsonUtils.toJsonString(childFileSourceDTO));
                fileEntity.setAgentIp(agentIp);
                fileEntity.setUuid(uuid);
                fileEntity.setSourceName(fileEntity.getSourceName() + "-" + RandomStringUtils.randomAlphanumeric(10));
                fileEntity.setTemplateId(sourceEntity.getId());
                int op = getOp(fileEntity.getStatus());
                int nextStatus = getNextStatus(fileEntity.getStatus());
                fileEntity.setStatus(nextStatus);
                if (sourceMapper.insert(fileEntity) > 0) {
                    fileTasks.add(getDataConfig(fileEntity, op));
                }
            }
            if (fileTasks.size() >= TASK_FETCH_SIZE) {
                break;
            }
        }
        return fileTasks;
    }

    private List<DataConfig> fetchIssuedTasks(TaskRequest taskRequest) {
        final String agentIp = taskRequest.getAgentIp();
        final String uuid = taskRequest.getUuid();
        List<Integer> statusList = Arrays.asList(SourceStatus.TO_BE_ISSUED_DELETE.getCode(),
                SourceStatus.TO_BE_ISSUED_RETRY.getCode(), SourceStatus.TO_BE_ISSUED_BACKTRACK.getCode(),
                SourceStatus.TO_BE_ISSUED_FROZEN.getCode(), SourceStatus.TO_BE_ISSUED_CHECK.getCode(),
                SourceStatus.TO_BE_ISSUED_REDO_METRIC.getCode(), SourceStatus.TO_BE_ISSUED_MAKEUP.getCode());
        List<StreamSourceEntity> sourceEntities = sourceMapper.selectByStatusAndIp(statusList, agentIp, uuid);
        List<DataConfig> issuedTasks = Lists.newArrayList();
        for (StreamSourceEntity issuedTask : sourceEntities) {
            int op = getOp(issuedTask.getStatus());
            int nextStatus = getNextStatus(issuedTask.getStatus());
            issuedTask.setStatus(nextStatus);
            if (sourceMapper.updateByPrimaryKeySelective(issuedTask) == 1) {
                issuedTask.setVersion(issuedTask.getVersion() + 1);
                issuedTasks.add(getDataConfig(issuedTask, op));
            }
        }
        return issuedTasks;
    }

    private int getOp(int status) {
        return status % MODULUS_100;
    }

    private int getNextStatus(int status) {
        int op = status % MODULUS_100;
        return ISSUED_STATUS * MODULUS_100 + op;
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
        dataConfig.setVersion(entity.getVersion());

        String groupId = entity.getInlongGroupId();
        String streamId = entity.getInlongStreamId();
        dataConfig.setInlongGroupId(groupId);
        dataConfig.setInlongStreamId(streamId);
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        String extParams = entity.getExtParams();
        if (streamEntity != null) {
            dataConfig.setSyncSend(streamEntity.getSyncSend());
            if (SourceType.FILE.equalsIgnoreCase(streamEntity.getDataType())) {
                String dataSeparator = streamEntity.getDataSeparator();
                extParams = null != dataSeparator ? getExtParams(extParams, dataSeparator) : extParams;
            }
        } else {
            dataConfig.setSyncSend(0);
            LOGGER.warn("set syncSend=[0] as the stream not exists for groupId={}, streamId={}", groupId, streamId);
        }
        dataConfig.setExtParams(extParams);
        return dataConfig;
    }

    private String getExtParams(String extParams, String dataSeparator) {
        if (Objects.isNull(extParams)) {
            return null;
        }
        FileSourceDTO fileSourceDTO = JsonUtils.parseObject(extParams, FileSourceDTO.class);
        if (Objects.nonNull(fileSourceDTO)) {
            fileSourceDTO.setDataSeparator(dataSeparator);
            return JsonUtils.toJsonString(fileSourceDTO);
        }
        return extParams;
    }

    /**
     * Get the Task type from the stream source entity.
     *
     * @param sourceEntity stream source info.
     * @return task type
     */
    private int getTaskType(StreamSourceEntity sourceEntity) {
        TaskTypeEnum taskType = SourceType.SOURCE_TASK_MAP.get(sourceEntity.getSourceType());
        if (taskType == null) {
            throw new BusinessException("Unsupported task type for source type " + sourceEntity.getSourceType());
        }
        return taskType.getType();
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
