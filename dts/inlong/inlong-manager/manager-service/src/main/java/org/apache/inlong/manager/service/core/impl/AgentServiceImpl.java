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

import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.common.db.CommandEntity;
import org.apache.inlong.common.enums.PullJobTypeEnum;
import org.apache.inlong.common.enums.TaskTypeEnum;
import org.apache.inlong.common.pojo.agent.CmdConfig;
import org.apache.inlong.common.pojo.agent.DataConfig;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.apache.inlong.common.pojo.dataproxy.DataProxyTopicInfo;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongClusterEntity;
import org.apache.inlong.manager.dao.entity.InlongClusterNodeEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.StreamSourceEntity;
import org.apache.inlong.manager.dao.mapper.DataSourceCmdConfigEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongClusterNodeEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.StreamSourceEntityMapper;
import org.apache.inlong.manager.pojo.cluster.ClusterPageRequest;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeBindGroupRequest;
import org.apache.inlong.manager.pojo.cluster.agent.AgentClusterNodeDTO;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterDTO;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarDTO;
import org.apache.inlong.manager.pojo.source.file.FileSourceDTO;
import org.apache.inlong.manager.service.core.AgentService;
import org.apache.inlong.manager.service.source.SourceSnapshotOperator;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.set.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.inlong.manager.common.consts.InlongConstants.DOT;

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
    private static final Gson GSON = new Gson();
    private final ExecutorService executorService = new ThreadPoolExecutor(
            5,
            10,
            10L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat("async-agent-%s").build(),
            new CallerRunsPolicy());
    @Value("${source.update.enabled:false}")
    private Boolean updateTaskTimeoutEnabled;
    @Value("${source.update.before.seconds:60}")
    private Integer beforeSeconds;
    @Value("${source.update.interval:60}")
    private Integer updateTaskInterval;
    @Value("${source.cleansing.enabled:false}")
    private Boolean sourceCleanEnabled;
    @Value("${source.cleansing.interval:600}")
    private Integer cleanInterval;
    @Autowired
    private StreamSourceEntityMapper sourceMapper;
    @Autowired
    private SourceSnapshotOperator snapshotOperator;
    @Autowired
    private DataSourceCmdConfigEntityMapper sourceCmdConfigMapper;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private InlongClusterEntityMapper clusterMapper;
    @Autowired
    private InlongClusterNodeEntityMapper clusterNodeMapper;

    /**
     * Start the update task
     */
    @PostConstruct
    private void startHeartbeatTask() {
        if (updateTaskTimeoutEnabled) {
            ThreadFactory factory = new ThreadFactoryBuilder()
                    .setNameFormat("scheduled-source-timeout-%d")
                    .setDaemon(true)
                    .build();
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(factory);
            executor.scheduleWithFixedDelay(() -> {
                try {
                    sourceMapper.updateStatusToTimeout(beforeSeconds);
                    LOGGER.info("update task status successfully");
                } catch (Throwable t) {
                    LOGGER.error("update task status error", t);
                }
            }, 0, updateTaskInterval, TimeUnit.SECONDS);
            LOGGER.info("update task status started successfully");
        }
        if (sourceCleanEnabled) {
            ThreadFactory factory = new ThreadFactoryBuilder()
                    .setNameFormat("scheduled-source-deleted-%d")
                    .setDaemon(true)
                    .build();
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(factory);
            executor.scheduleWithFixedDelay(() -> {
                try {
                    sourceMapper.updateStatusByDeleted();
                    LOGGER.info("clean task successfully");
                } catch (Throwable t) {
                    LOGGER.error("clean task error", t);
                }
            }, 0, cleanInterval, TimeUnit.SECONDS);
            LOGGER.info("clean task started successfully");
        }
    }

    @Override
    public Boolean reportSnapshot(TaskSnapshotRequest request) {
        return snapshotOperator.snapshot(request);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void report(TaskRequest request) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("begin to get agent task: {}", request);
        }
        if (request == null || StringUtils.isBlank(request.getAgentIp())) {
            throw new BusinessException("agent request or agent ip was empty, just return");
        }

        preTimeoutTasks(request);

        // Update task status, other tasks with status 20x will change to 30x in next request
        if (CollectionUtils.isEmpty(request.getCommandInfo())) {
            LOGGER.debug("task result was empty in request: {}, just return", request);
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
            if (SourceStatus.BEEN_ISSUED_DELETE.getCode() == previousStatus) {
                nextStatus = SourceStatus.SOURCE_DISABLE.getCode();
            } else if (SourceStatus.BEEN_ISSUED_STOP.getCode() == previousStatus) {
                nextStatus = SourceStatus.SOURCE_STOP.getCode();
            }
        }

        if (nextStatus != previousStatus) {
            sourceMapper.updateStatus(taskId, nextStatus, false);
            LOGGER.info("task result=[{}], update source status to [{}] for id [{}]", result, nextStatus, taskId);
        }
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public TaskResult getTaskResult(TaskRequest request) {
        if (StringUtils.isBlank(request.getClusterName()) || StringUtils.isBlank(request.getAgentIp())) {
            throw new BusinessException("agent request or agent ip was empty, just return");
        }

        preProcessFileTask(request);
        preProcessNonFileTasks(request);
        List<DataConfig> tasks = processQueuedTasks(request);

        // Query pending special commands
        List<CmdConfig> cmdConfigs = getAgentCmdConfigs(request);
        return TaskResult.builder().dataConfigs(tasks).cmdConfigs(cmdConfigs).build();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public Boolean bindGroup(AgentClusterNodeBindGroupRequest request) {
        HashSet<String> bindSet = Sets.newHashSet();
        HashSet<String> unbindSet = Sets.newHashSet();
        if (request.getBindClusterNodes() != null) {
            bindSet.addAll(request.getBindClusterNodes());
        }
        if (request.getUnbindClusterNodes() != null) {
            unbindSet.addAll(request.getUnbindClusterNodes());
        }
        Preconditions.expectTrue(Sets.union(bindSet, unbindSet).size() == bindSet.size() + unbindSet.size(),
                "can not add and del node tag in the sameTime");
        InlongClusterEntity cluster = clusterMapper.selectByNameAndType(request.getClusterName(), ClusterType.AGENT);
        String message = "Current user does not have permission to bind cluster node tag";

        if (CollectionUtils.isNotEmpty(bindSet)) {
            bindSet.stream().flatMap(clusterNode -> {
                ClusterPageRequest pageRequest = new ClusterPageRequest();
                pageRequest.setParentId(cluster.getId());
                pageRequest.setType(ClusterType.AGENT);
                pageRequest.setKeyword(clusterNode);
                return clusterNodeMapper.selectByCondition(pageRequest).stream();
            }).filter(Objects::nonNull)
                    .forEach(entity -> {
                        Set<String> groupSet = new HashSet<>();
                        AgentClusterNodeDTO agentClusterNodeDTO = new AgentClusterNodeDTO();
                        if (StringUtils.isNotBlank(entity.getExtParams())) {
                            agentClusterNodeDTO = AgentClusterNodeDTO.getFromJson(entity.getExtParams());
                            String agentGroup = agentClusterNodeDTO.getAgentGroup();
                            groupSet = StringUtils.isBlank(agentGroup) ? groupSet
                                    : Sets.newHashSet(agentGroup.split(InlongConstants.COMMA));
                        }
                        groupSet.add(request.getAgentGroup());
                        agentClusterNodeDTO.setAgentGroup(Joiner.on(",").join(groupSet));
                        entity.setExtParams(GSON.toJson(agentClusterNodeDTO));
                        clusterNodeMapper.insertOnDuplicateKeyUpdate(entity);
                    });
        }

        if (CollectionUtils.isNotEmpty(unbindSet)) {
            unbindSet.stream().flatMap(clusterNode -> {
                ClusterPageRequest pageRequest = new ClusterPageRequest();
                pageRequest.setParentId(cluster.getId());
                pageRequest.setType(ClusterType.AGENT);
                pageRequest.setKeyword(clusterNode);
                return clusterNodeMapper.selectByCondition(pageRequest).stream();
            }).filter(Objects::nonNull)
                    .forEach(entity -> {
                        Set<String> groupSet = new HashSet<>();
                        AgentClusterNodeDTO agentClusterNodeDTO = new AgentClusterNodeDTO();
                        if (StringUtils.isNotBlank(entity.getExtParams())) {
                            agentClusterNodeDTO = AgentClusterNodeDTO.getFromJson(entity.getExtParams());
                            String agentGroup = agentClusterNodeDTO.getAgentGroup();
                            groupSet = StringUtils.isBlank(agentGroup) ? groupSet
                                    : Sets.newHashSet(agentGroup.split(InlongConstants.COMMA));
                        }
                        groupSet.remove(request.getAgentGroup());
                        agentClusterNodeDTO.setAgentGroup(Joiner.on(",").join(groupSet));
                        entity.setExtParams(GSON.toJson(agentClusterNodeDTO));
                        clusterNodeMapper.insertOnDuplicateKeyUpdate(entity);
                    });
        }
        return true;
    }

    /**
     * Query the tasks that source is waited to be operated.(only clusterName and ip matched it can be operated)
     */
    private List<DataConfig> processQueuedTasks(TaskRequest request) {
        HashSet<SourceStatus> needAddStatusSet = Sets.newHashSet(SourceStatus.TOBE_ISSUED_SET);
        if (PullJobTypeEnum.NEVER == PullJobTypeEnum.getPullJobType(request.getPullJobType())) {
            LOGGER.debug("agent pull job type is [NEVER], just pull to be active tasks");
            needAddStatusSet.remove(SourceStatus.TO_BE_ISSUED_ADD);
        }

        // todo:Find out why uuid is necessary here, if it is invalid, ignore it
        List<StreamSourceEntity> sourceEntities = sourceMapper.selectByStatusAndCluster(
                needAddStatusSet.stream().map(SourceStatus::getCode).collect(Collectors.toList()),
                request.getClusterName(), request.getAgentIp(), request.getUuid());
        List<DataConfig> issuedTasks = Lists.newArrayList();
        for (StreamSourceEntity sourceEntity : sourceEntities) {
            int op = getOp(sourceEntity.getStatus());
            int nextStatus = getNextStatus(sourceEntity.getStatus());
            sourceEntity.setPreviousStatus(sourceEntity.getStatus());
            sourceEntity.setStatus(nextStatus);
            if (sourceMapper.updateByPrimaryKeySelective(sourceEntity) == 1) {
                sourceEntity.setVersion(sourceEntity.getVersion() + 1);
                DataConfig dataConfig = getDataConfig(sourceEntity, op);
                issuedTasks.add(dataConfig);
                LOGGER.info("Offer source task({}) for agent({}) in cluster({})",
                        dataConfig, request.getAgentIp(), request.getClusterName());
            }
        }
        return issuedTasks;
    }

    // todo:If many agents pull the same non-file task in this place, wouldn’t it be a problem?
    // it will issue multiple tasks
    private void preProcessNonFileTasks(TaskRequest taskRequest) {
        List<Integer> needAddStatusList;
        if (PullJobTypeEnum.NEVER == PullJobTypeEnum.getPullJobType(taskRequest.getPullJobType())) {
            LOGGER.debug("agent pull job type is [NEVER], just pull to be active tasks");
            needAddStatusList = Collections.singletonList(SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        } else {
            needAddStatusList = Arrays.asList(SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                    SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        }
        List<String> sourceTypes = Lists.newArrayList(SourceType.MYSQL_SQL, SourceType.KAFKA,
                SourceType.MYSQL_BINLOG, SourceType.POSTGRESQL);
        List<StreamSourceEntity> sourceEntities = sourceMapper.selectByStatusAndType(needAddStatusList, sourceTypes,
                TASK_FETCH_SIZE);
        for (StreamSourceEntity sourceEntity : sourceEntities) {
            // refresh agent ip and uuid to make it can be processed in queued task
            sourceEntity.setAgentIp(taskRequest.getAgentIp());
            sourceEntity.setUuid(taskRequest.getUuid());
            sourceMapper.updateByPrimaryKeySelective(sourceEntity);
        }
    }

    private void preProcessFileTask(TaskRequest taskRequest) {
        preProcessTemplateFileTask(taskRequest);
        preProcessLabelFileTasks(taskRequest);
    }

    /**
     * Add subtasks to template tasks.
     * (Template task are agent_ip is null and template_id is null)
     */
    private void preProcessTemplateFileTask(TaskRequest taskRequest) {
        List<Integer> needCopiedStatusList = Arrays.asList(SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        final String agentIp = taskRequest.getAgentIp();
        final String agentClusterName = taskRequest.getClusterName();
        Preconditions.expectTrue(StringUtils.isNotBlank(agentIp) || StringUtils.isNotBlank(agentClusterName),
                "both agent ip and cluster name are blank when fetching file task");

        // find those node whose tag match stream_source tag and agent ip match stream_source agent ip
        List<StreamSourceEntity> sourceEntities = sourceMapper.selectTemplateSourceByCluster(needCopiedStatusList,
                Lists.newArrayList(SourceType.FILE), agentClusterName);
        Set<GroupStatus> noNeedAddTask = Sets.newHashSet(
                GroupStatus.SUSPENDED, GroupStatus.SUSPENDING, GroupStatus.DELETING, GroupStatus.DELETED);
        sourceEntities.stream()
                .forEach(sourceEntity -> {
                    InlongGroupEntity groupEntity = groupMapper.selectByGroupId(sourceEntity.getInlongGroupId());
                    if (groupEntity != null && noNeedAddTask.contains(GroupStatus.forCode(groupEntity.getStatus()))) {
                        return;
                    }
                    StreamSourceEntity subSource = sourceMapper.selectOneByTemplatedIdAndAgentIp(sourceEntity.getId(),
                            agentIp);
                    if (subSource == null) {
                        InlongClusterNodeEntity clusterNodeEntity = selectByIpAndCluster(agentClusterName, agentIp);
                        // if stream_source match node_group with node, clone a subtask for this Agent.
                        // note: a new source name with random suffix is generated to adhere to the unique constraint
                        if (matchGroup(sourceEntity, clusterNodeEntity)) {
                            StreamSourceEntity fileEntity =
                                    CommonBeanUtils.copyProperties(sourceEntity, StreamSourceEntity::new);
                            fileEntity.setSourceName(fileEntity.getSourceName() + "-"
                                    + RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT));
                            fileEntity.setTemplateId(sourceEntity.getId());
                            fileEntity.setAgentIp(agentIp);
                            fileEntity.setStatus(SourceStatus.TO_BE_ISSUED_ADD.getCode());
                            // create new sub source task
                            sourceMapper.insert(fileEntity);
                            LOGGER.info("Transform new template task({}) for agent({}) in cluster({}).",
                                    fileEntity.getId(), taskRequest.getAgentIp(), taskRequest.getClusterName());
                        }
                    }
                });
    }

    /**
     * Find file collecting task match those condition:
     * 1.agent ip match
     * 2.cluster name match
     * Send the corresponding task action request according to the matching state of the tag and the current state
     */
    private void preProcessLabelFileTasks(TaskRequest taskRequest) {
        List<Integer> needProcessedStatusList = Arrays.asList(
                SourceStatus.SOURCE_NORMAL.getCode(),
                SourceStatus.SOURCE_FAILED.getCode(),
                SourceStatus.SOURCE_STOP.getCode(),
                SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                SourceStatus.TO_BE_ISSUED_STOP.getCode(),
                SourceStatus.TO_BE_ISSUED_ACTIVE.getCode());
        final String agentIp = taskRequest.getAgentIp();
        final String agentClusterName = taskRequest.getClusterName();
        Preconditions.expectTrue(StringUtils.isNotBlank(agentIp) || StringUtils.isNotBlank(agentClusterName),
                "both agent ip and cluster name are blank when fetching file task");

        InlongClusterNodeEntity clusterNodeEntity = selectByIpAndCluster(agentClusterName, agentIp);
        List<StreamSourceEntity> sourceEntities = sourceMapper.selectByAgentIpAndCluster(needProcessedStatusList,
                Lists.newArrayList(SourceType.FILE), agentIp, agentClusterName);

        sourceEntities.forEach(sourceEntity -> {
            // case: agent tag unbind and mismatch source task
            Set<SourceStatus> exceptedUnmatchedStatus = Sets.newHashSet(
                    SourceStatus.SOURCE_STOP,
                    SourceStatus.TO_BE_ISSUED_STOP);
            if (!matchGroup(sourceEntity, clusterNodeEntity)
                    && !exceptedUnmatchedStatus.contains(SourceStatus.forCode(sourceEntity.getStatus()))) {
                LOGGER.info("Transform task({}) from {} to {} because tag mismatch "
                        + "for agent({}) in cluster({})", sourceEntity.getAgentIp(),
                        sourceEntity.getStatus(), SourceStatus.TO_BE_ISSUED_STOP.getCode(),
                        agentIp, agentClusterName);
                sourceMapper.updateStatus(
                        sourceEntity.getId(), SourceStatus.TO_BE_ISSUED_STOP.getCode(), false);
            }

            // case: agent tag rebind and match source task again and stream is not in 'SUSPENDED' status
            InlongGroupEntity groupEntity = groupMapper.selectByGroupId(sourceEntity.getInlongGroupId());
            Set<SourceStatus> exceptedMatchedSourceStatus = Sets.newHashSet(
                    SourceStatus.SOURCE_NORMAL,
                    SourceStatus.TO_BE_ISSUED_ADD,
                    SourceStatus.TO_BE_ISSUED_ACTIVE);
            Set<GroupStatus> matchedGroupStatus = Sets.newHashSet(
                    GroupStatus.CONFIG_SUCCESSFUL, GroupStatus.RESTARTED);
            if (matchGroup(sourceEntity, clusterNodeEntity)
                    && groupEntity != null
                    && !exceptedMatchedSourceStatus.contains(SourceStatus.forCode(sourceEntity.getStatus()))
                    && matchedGroupStatus.contains(GroupStatus.forCode(groupEntity.getStatus()))) {
                LOGGER.info("Transform task({}) from {} to {} because tag rematch "
                        + "for agent({}) in cluster({})", sourceEntity.getAgentIp(),
                        sourceEntity.getStatus(), SourceStatus.TO_BE_ISSUED_ACTIVE.getCode(),
                        agentIp, agentClusterName);
                sourceMapper.updateStatus(
                        sourceEntity.getId(), SourceStatus.TO_BE_ISSUED_ACTIVE.getCode(), false);
            }
        });
    }

    private void preTimeoutTasks(TaskRequest taskRequest) {
        // If the agent report succeeds, restore the source status
        List<Integer> needUpdateIds = sourceMapper.selectHeartbeatTimeoutIds(null, taskRequest.getAgentIp(),
                taskRequest.getClusterName());
        // restore state for all source by ip and type
        if (CollectionUtils.isNotEmpty(needUpdateIds)) {
            sourceMapper.rollbackTimeoutStatusByIds(needUpdateIds, null);
        }
    }

    private InlongClusterNodeEntity selectByIpAndCluster(String clusterName, String ip) {
        InlongClusterEntity clusterEntity = clusterMapper.selectByNameAndType(clusterName, ClusterType.AGENT);
        if (clusterEntity == null) {
            return null;
        }
        return clusterNodeMapper.selectByParentIdAndIp(clusterEntity.getId(), ip).stream().findFirst().orElse(null);
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

        InlongGroupEntity groupEntity = groupMapper.selectByGroupId(groupId);
        InlongStreamEntity streamEntity = streamMapper.selectByIdentifier(groupId, streamId);
        String extParams = entity.getExtParams();
        if (groupEntity != null && streamEntity != null) {
            dataConfig.setSyncSend(streamEntity.getSyncSend());
            if (SourceType.FILE.equalsIgnoreCase(streamEntity.getDataType())) {
                String dataSeparator = streamEntity.getDataSeparator();
                extParams = (null != dataSeparator ? getExtParams(extParams, dataSeparator) : extParams);
            }

            int dataReportType = groupEntity.getDataReportType();
            dataConfig.setDataReportType(dataReportType);
            if (InlongConstants.REPORT_TO_MQ_RECEIVED == dataReportType) {
                // add mq cluster setting
                List<MQClusterInfo> mqSet = new ArrayList<>();
                List<String> clusterTagList = Collections.singletonList(groupEntity.getInlongClusterTag());
                ClusterPageRequest pageRequest = ClusterPageRequest.builder()
                        .type(groupEntity.getMqType())
                        .clusterTagList(clusterTagList)
                        .build();
                List<InlongClusterEntity> mqClusterList = clusterMapper.selectByCondition(pageRequest);
                for (InlongClusterEntity cluster : mqClusterList) {
                    MQClusterInfo clusterInfo = new MQClusterInfo();
                    clusterInfo.setUrl(cluster.getUrl());
                    clusterInfo.setToken(cluster.getToken());
                    clusterInfo.setMqType(cluster.getType());
                    clusterInfo.setParams(JsonUtils.parseObject(cluster.getExtParams(), HashMap.class));
                    mqSet.add(clusterInfo);
                }
                dataConfig.setMqClusters(mqSet);

                // add topic setting
                String mqResource = groupEntity.getMqResource();
                String mqType = groupEntity.getMqType();
                if (MQType.PULSAR.equals(mqType) || MQType.TDMQ_PULSAR.equals(mqType)) {
                    // first get the tenant from the InlongGroup, and then get it from the PulsarCluster.
                    InlongPulsarDTO pulsarDTO = InlongPulsarDTO.getFromJson(groupEntity.getExtParams());
                    String tenant = pulsarDTO.getPulsarTenant();
                    if (StringUtils.isBlank(tenant)) {
                        // If there are multiple Pulsar clusters, take the first one.
                        // Note that the tenants in multiple Pulsar clusters must be identical.
                        PulsarClusterDTO pulsarCluster = PulsarClusterDTO.getFromJson(
                                mqClusterList.get(0).getExtParams());
                        tenant = pulsarCluster.getPulsarTenant();
                    }

                    String topic = String.format(InlongConstants.PULSAR_TOPIC_FORMAT,
                            tenant, mqResource, streamEntity.getMqResource());
                    DataProxyTopicInfo topicConfig = new DataProxyTopicInfo();
                    topicConfig.setInlongGroupId(groupId + "/" + streamId);
                    topicConfig.setTopic(topic);
                    dataConfig.setTopicInfo(topicConfig);
                } else if (MQType.TUBEMQ.equals(mqType)) {
                    DataProxyTopicInfo topicConfig = new DataProxyTopicInfo();
                    topicConfig.setInlongGroupId(groupId);
                    topicConfig.setTopic(mqResource);
                    dataConfig.setTopicInfo(topicConfig);
                } else if (MQType.KAFKA.equals(mqType)) {
                    DataProxyTopicInfo topicConfig = new DataProxyTopicInfo();
                    topicConfig.setInlongGroupId(groupId);
                    topicConfig.setTopic(groupEntity.getMqResource() + DOT + streamEntity.getMqResource());
                    dataConfig.setTopicInfo(topicConfig);
                }
            } else {
                LOGGER.warn("set syncSend=[0] as the stream not exists for groupId={}, streamId={}", groupId, streamId);
            }
        }
        dataConfig.setExtParams(extParams);
        return dataConfig;
    }

    private String getExtParams(String extParams, String dataSeparator) {
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

    // todo:delete it, source cmd is useless

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

    private boolean matchGroup(StreamSourceEntity sourceEntity, InlongClusterNodeEntity clusterNodeEntity) {
        Preconditions.expectNotNull(sourceEntity, "cluster must be valid");
        if (sourceEntity.getInlongClusterNodeGroup() == null) {
            return true;
        }

        if (clusterNodeEntity == null || clusterNodeEntity.getExtParams() == null) {
            return false;
        }

        Set<String> clusterNodeGroups = new HashSet<>();
        if (StringUtils.isNotBlank(clusterNodeEntity.getExtParams())) {
            AgentClusterNodeDTO agentClusterNodeDTO = AgentClusterNodeDTO.getFromJson(clusterNodeEntity.getExtParams());
            String agentGroup = agentClusterNodeDTO.getAgentGroup();
            clusterNodeGroups = StringUtils.isBlank(agentGroup) ? new HashSet<>()
                    : Sets.newHashSet(agentGroup.split(InlongConstants.COMMA));
        }
        Set<String> sourceGroups = Stream.of(
                sourceEntity.getInlongClusterNodeGroup().split(InlongConstants.COMMA)).collect(Collectors.toSet());
        return sourceGroups.stream().anyMatch(clusterNodeGroups::contains);
    }

}
