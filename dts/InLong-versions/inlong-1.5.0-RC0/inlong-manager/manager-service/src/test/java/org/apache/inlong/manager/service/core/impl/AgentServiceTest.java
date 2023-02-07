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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.common.constant.Constants;
import org.apache.inlong.common.db.CommandEntity;
import org.apache.inlong.common.enums.ManagerOpEnum;
import org.apache.inlong.common.enums.PullJobTypeEnum;
import org.apache.inlong.common.pojo.agent.DataConfig;
import org.apache.inlong.common.pojo.agent.TaskRequest;
import org.apache.inlong.common.pojo.agent.TaskResult;
import org.apache.inlong.common.pojo.agent.TaskSnapshotMessage;
import org.apache.inlong.common.pojo.agent.TaskSnapshotRequest;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.file.FileSourceRequest;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSourceRequest;
import org.apache.inlong.manager.service.ServiceBaseTest;
import org.apache.inlong.manager.service.core.AgentService;
import org.apache.inlong.manager.service.core.HeartbeatService;
import org.apache.inlong.manager.service.group.InlongGroupProcessService;
import org.apache.inlong.manager.service.mocks.MockAgent;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agent service test
 */
class AgentServiceTest extends ServiceBaseTest {

    private static MockAgent agent;
    @Autowired
    private StreamSourceService sourceService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private HeartbeatService heartbeatService;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongStreamServiceTest streamServiceTest;
    @Autowired
    private InlongStreamEntityMapper streamMapper;

    private List<Pair<String, String>> groupStreamCache;
    private List<String> groupCache;

    /**
     * Save template source
     */
    public Integer saveTemplateSource() {
        streamServiceTest.saveInlongStream(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID, GLOBAL_OPERATOR);
        FileSourceRequest sourceInfo = new FileSourceRequest();
        sourceInfo.setInlongGroupId(GLOBAL_GROUP_ID);
        sourceInfo.setInlongStreamId(GLOBAL_STREAM_ID);
        sourceInfo.setSourceType(SourceType.FILE);
        sourceInfo.setSourceName("template_source_in_agent_service_test");
        sourceInfo.setInlongClusterName(GLOBAL_CLUSTER_NAME);
        return sourceService.save(sourceInfo, GLOBAL_OPERATOR);
    }

    /**
     * Save source info.
     */
    public Integer saveSource() {
        streamServiceTest.saveInlongStream(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID, GLOBAL_OPERATOR);
        MySQLBinlogSourceRequest sourceInfo = new MySQLBinlogSourceRequest();
        sourceInfo.setInlongGroupId(GLOBAL_GROUP_ID);
        sourceInfo.setInlongStreamId(GLOBAL_STREAM_ID);
        sourceInfo.setSourceType(SourceType.MYSQL_BINLOG);
        sourceInfo.setSourceName("binlog_source_in_agent_service_test");
        return sourceService.save(sourceInfo, GLOBAL_OPERATOR);
    }

    /**
     * mock {@link StreamSourceService#save}
     */
    public Pair<String, String> saveSource(String group) {
        String groupId = UUID.randomUUID().toString();
        String streamId = UUID.randomUUID().toString();
        groupStreamCache.add(new ImmutablePair<>(groupId, streamId));
        streamServiceTest.saveInlongStream(groupId, streamId, GLOBAL_OPERATOR);

        FileSourceRequest sourceInfo = new FileSourceRequest();
        sourceInfo.setInlongGroupId(groupId);
        sourceInfo.setInlongStreamId(streamId);
        sourceInfo.setSourceType(SourceType.FILE);
        sourceInfo.setInlongClusterName(MockAgent.CLUSTER_NAME);
        sourceInfo.setInlongClusterNodeGroup(group);
        sourceInfo.setSourceName(
                String.format("Source task for cluster(%s) and group(%s)", MockAgent.CLUSTER_NAME, group));
        sourceService.save(sourceInfo, GLOBAL_OPERATOR);
        sourceService.updateStatus(
                groupId,
                streamId,
                SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                GLOBAL_OPERATOR);
        return Pair.of(groupId, streamId);
    }

    /**
     * mock {@link InlongGroupProcessService#suspendProcessAsync}, it will suspend stream source
     */
    public void suspendSource(String groupId, String streamId) {
        List<StreamSource> sources = sourceService.listSource(groupId, streamId);
        sources.stream()
                .filter(source -> source.getTemplateId() != null)
                .forEach(source -> sourceService.stop(source.getId(), GLOBAL_OPERATOR));
        streamMapper.updateStatusByIdentifier(groupId, streamId, StreamStatus.SUSPENDED.getCode(), GLOBAL_OPERATOR);
    }

    /**
     * mock {@link InlongGroupProcessService#restartProcessAsync}, it will restart stream source
     */
    public void restartSource(String groupId, String streamId) {
        List<StreamSource> sources = sourceService.listSource(groupId, streamId);
        sources.stream()
                .filter(source -> source.getTemplateId() != null)
                .forEach(source -> sourceService.restart(source.getId(), GLOBAL_OPERATOR));
        streamMapper.updateStatusByIdentifier(groupId, streamId, StreamStatus.RESTARTED.getCode(), GLOBAL_OPERATOR);
    }

    public void deleteSource(String groupId, String streamId) {
        sourceService.logicDeleteAll(groupId, streamId, GLOBAL_OPERATOR);
    }

    @BeforeAll
    public static void setUp(
            @Autowired AgentService agentService,
            @Autowired HeartbeatService heartbeatService) {
        agent = new MockAgent(agentService, heartbeatService, 2);
        agent.sendHeartbeat();
    }

    @BeforeEach
    public void setupEach() {
        groupStreamCache = new ArrayList<>();
        groupCache = new ArrayList<>();
    }

    @AfterEach
    public void teardownEach() {
        if (!groupStreamCache.isEmpty()) {
            groupStreamCache.forEach(groupStream -> sourceService.deleteAll(groupStream.getLeft(),
                    groupStream.getRight(), GLOBAL_OPERATOR));
            groupMapper.deleteByInlongGroupIds(
                    groupStreamCache.stream().map(Pair::getKey).collect(Collectors.toList()));
            streamMapper.deleteByInlongGroupIds(
                    groupStreamCache.stream().map(Pair::getValue).collect(Collectors.toList()));
        }
        groupStreamCache.clear();
        groupCache.stream().forEach(group -> bindGroup(false, group));;
    }

    private void bindGroup(boolean bind, String group) {
        if (bind) {
            groupCache.add(group);
        }
        agent.bindGroup(bind, group);
    }

    /**
     * Test bind group for node.
     */
    @Test
    public void testGroupMatch() {
        saveSource("group1,group3");
        saveSource("group2,group3");
        saveSource("group2,group3");
        saveSource("group4");
        bindGroup(true, "group1");
        bindGroup(true, "group2");

        TaskResult taskResult = agent.pullTask();
        Assertions.assertTrue(taskResult.getCmdConfigs().isEmpty());
        Assertions.assertEquals(4, taskResult.getDataConfigs().size());
        Assertions.assertEquals(3, taskResult.getDataConfigs().stream()
                .filter(dataConfig -> Integer.valueOf(dataConfig.getOp()) == ManagerOpEnum.ADD.getType())
                .collect(Collectors.toSet())
                .size());
        Assertions.assertEquals(1, taskResult.getDataConfigs().stream()
                .filter(dataConfig -> Integer.valueOf(dataConfig.getOp()) == ManagerOpEnum.FROZEN.getType())
                .collect(Collectors.toSet())
                .size());
    }

    /**
     * Test node group mismatch source task and next time rematch source task.
     */
    @Test
    public void testGroupMismatchAndRematch() {
        final Pair<String, String> groupStream = saveSource("group1,group3");
        bindGroup(true, "group1");

        agent.pullTask();
        agent.pullTask(); // report last success status

        final int sourceId = sourceService.listSource(groupStream.getLeft(), groupStream.getRight()).stream()
                .filter(source -> source.getTemplateId() != null)
                .findAny()
                .get()
                .getId();
        // unbind group and mismatch
        bindGroup(false, "group1");
        TaskResult t1 = agent.pullTask();
        Assertions.assertEquals(1, t1.getDataConfigs().size());
        Assertions.assertEquals(1, t1.getDataConfigs().stream()
                .filter(dataConfig -> Integer.valueOf(dataConfig.getOp()) == ManagerOpEnum.FROZEN.getType())
                .collect(Collectors.toSet())
                .size());
        DataConfig d1 = t1.getDataConfigs().get(0);
        Assertions.assertEquals(sourceId, d1.getTaskId());

        // bind group and rematch
        bindGroup(true, "group1");
        TaskResult t2 = agent.pullTask();
        Assertions.assertEquals(1, t2.getDataConfigs().size());
        Assertions.assertEquals(1, t2.getDataConfigs().stream()
                .filter(dataConfig -> Integer.valueOf(dataConfig.getOp()) == ManagerOpEnum.ACTIVE.getType())
                .collect(Collectors.toSet())
                .size());
        DataConfig d2 = t2.getDataConfigs().get(0);
        Assertions.assertEquals(sourceId, d2.getTaskId());
    }

    /**
     * Test suspend group when frozen task without ack.
     */
    @Test
    public void testSuspendFailWhenNotAck() {
        Pair<String, String> groupStream = saveSource("group1,group3");
        bindGroup(true, "group1");

        agent.pullTask();
        agent.pullTask(); // report last success status

        // mismatch
        bindGroup(false, "group1");
        agent.pullTask();

        // suspend
        try {
            suspendSource(groupStream.getLeft(), groupStream.getRight());
        } catch (BusinessException e) {
            Assertions.assertTrue(e.getMessage().contains("is not allowed to stop"));
        }
    }

    /**
     * Test node group rematch source task but group suspend
     */
    @Test
    public void testRematchedWhenSuspend() {
        final Pair<String, String> groupStream = saveSource("group1,group3");
        bindGroup(true, "group1");

        agent.pullTask();
        agent.pullTask(); // report last success status

        // mismatch and rematch
        bindGroup(false, "group1");
        agent.pullTask();
        agent.pullTask(); // report last to make it from 304 -> 104
        bindGroup(true, "group1");

        // suspend
        suspendSource(groupStream.getLeft(), groupStream.getRight());
        TaskResult taskResult = agent.pullTask();
        Assertions.assertEquals(0, taskResult.getDataConfigs().size());
    }

    /**
     * Test node group mismatch source task but group restart
     */
    @Test
    public void testMismatchedWhenRestart() {
        final Pair<String, String> groupStream = saveSource("group1,group3");
        bindGroup(true, "group1");

        agent.pullTask();
        agent.pullTask(); // report last success status

        // suspend and restart
        suspendSource(groupStream.getLeft(), groupStream.getRight());
        restartSource(groupStream.getLeft(), groupStream.getRight());
        bindGroup(false, "group1");
        TaskResult taskResult = agent.pullTask();
        Assertions.assertEquals(1, taskResult.getDataConfigs().size());
        Assertions.assertEquals(1, taskResult.getDataConfigs().stream()
                .filter(dataConfig -> Integer.valueOf(dataConfig.getOp()) == ManagerOpEnum.FROZEN.getType())
                .collect(Collectors.toSet())
                .size());
    }

    /**
     * Test suspend group for source task.
     */
    @Test
    public void testDelete() {
        final Pair<String, String> groupStream = saveSource(null);
        agent.pullTask();
        agent.pullTask(); // report last success status

        // suspend
        deleteSource(groupStream.getLeft(), groupStream.getRight());
        TaskResult taskResult = agent.pullTask();
        Assertions.assertEquals(1, taskResult.getDataConfigs().size());
        Assertions.assertEquals(1, taskResult.getDataConfigs().stream()
                .filter(dataConfig -> Integer.valueOf(dataConfig.getOp()) == ManagerOpEnum.DEL.getType())
                .collect(Collectors.toSet())
                .size());
    }

    /**
     * Test report snapshot.
     */
    @Test
    void testReportSnapshot() {
        Integer id = this.saveSource();

        TaskSnapshotRequest request = new TaskSnapshotRequest();
        request.setAgentIp("127.0.0.1");
        request.setReportTime(new Date());

        TaskSnapshotMessage message = new TaskSnapshotMessage();
        message.setJobId(id);
        message.setSnapshot("{\"offset\": 100}");
        request.setSnapshotList(Collections.singletonList(message));

        Boolean result = agentService.reportSnapshot(request);
        Assertions.assertTrue(result);

        sourceService.delete(id, GLOBAL_OPERATOR);
    }

    /**
     * Test sub-source task status manipulation.
     */
    @Test
    void testGetAndReportSubSourceTask() {
        // create template source for cluster agents and approve
        final Integer templateId = this.saveTemplateSource();
        sourceService.updateStatus(GLOBAL_GROUP_ID, GLOBAL_STREAM_ID, SourceStatus.TO_BE_ISSUED_ADD.getCode(),
                GLOBAL_OPERATOR);

        // get sub-source task
        TaskRequest getRequest = new TaskRequest();
        getRequest.setAgentIp("127.0.0.1");
        getRequest.setClusterName(GLOBAL_CLUSTER_NAME);
        getRequest.setPullJobType(PullJobTypeEnum.NEW.getType());
        TaskResult result = agentService.getTaskResult(getRequest);
        Assertions.assertEquals(1, result.getDataConfigs().size());
        DataConfig subSourceTask = result.getDataConfigs().get(0);
        // new sub-source version must be 1
        Assertions.assertEquals(2, subSourceTask.getVersion());
        // sub-source's id must be different from its template source
        Assertions.assertNotEquals(templateId, subSourceTask.getTaskId());
        // operation is to add new task
        Assertions.assertEquals(SourceStatus.BEEN_ISSUED_ADD.getCode() % 100,
                Integer.valueOf(subSourceTask.getOp()));

        // report sub-source status
        CommandEntity reportTask = new CommandEntity();
        reportTask.setTaskId(subSourceTask.getTaskId());
        reportTask.setVersion(subSourceTask.getVersion());
        reportTask.setCommandResult(Constants.RESULT_SUCCESS);
        TaskRequest reportRequest = new TaskRequest();
        reportRequest.setAgentIp("127.0.0.1");
        reportRequest.setCommandInfo(Lists.newArrayList(reportTask));
        agentService.report(reportRequest);

        // check sub-source task status
        StreamSource subSource = sourceService.get(subSourceTask.getTaskId());
        Assertions.assertEquals(SourceStatus.SOURCE_NORMAL.getCode(), subSource.getStatus());

        sourceService.delete(templateId, GLOBAL_OPERATOR);
        sourceService.delete(subSource.getId(), GLOBAL_OPERATOR);
    }

}
