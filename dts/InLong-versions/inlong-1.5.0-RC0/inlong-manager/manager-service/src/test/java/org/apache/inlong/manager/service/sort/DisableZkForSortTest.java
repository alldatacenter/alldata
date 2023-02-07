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

package org.apache.inlong.manager.service.sort;

import com.google.common.collect.Lists;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.hive.HiveSinkRequest;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSourceRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.service.mocks.MockPlugin;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.service.workflow.WorkflowServiceImplTest;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.apache.inlong.manager.workflow.util.WorkflowUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test class for listen delete sort event.
 */
public class DisableZkForSortTest extends WorkflowServiceImplTest {

    @Autowired
    protected InlongStreamService streamService;

    @Autowired
    protected StreamSinkService streamSinkService;

    @Autowired
    protected StreamSourceService streamSourceService;

    @BeforeEach
    public void init() {
        subType = "disable_zk";
    }

    /**
     * Create Hive sink by inlong stream info.
     */
    public HiveSinkRequest createHiveSink(InlongStreamInfo streamInfo) {
        HiveSinkRequest sinkRequest = new HiveSinkRequest();
        sinkRequest.setInlongGroupId(streamInfo.getInlongGroupId());
        sinkRequest.setSinkType("HIVE");
        sinkRequest.setSinkName("HIVE");
        sinkRequest.setInlongStreamId(streamInfo.getInlongStreamId());
        List<SinkField> sinkFields = createStreamFields(streamInfo.getInlongGroupId(),
                streamInfo.getInlongStreamId())
                        .stream()
                        .map(streamField -> {
                            SinkField fieldInfo = new SinkField();
                            fieldInfo.setFieldName(streamField.getFieldName());
                            fieldInfo.setFieldType(streamField.getFieldType());
                            fieldInfo.setFieldComment(streamField.getFieldComment());
                            return fieldInfo;
                        })
                        .collect(Collectors.toList());
        sinkRequest.setSinkFieldList(sinkFields);
        sinkRequest.setEnableCreateResource(0);
        sinkRequest.setUsername(OPERATOR);
        sinkRequest.setPassword("password");
        sinkRequest.setDbName("default");
        sinkRequest.setTableName("kip_test");
        sinkRequest.setJdbcUrl("jdbc:hive2://localhost:7001");
        sinkRequest.setFileFormat("TextFile");
        sinkRequest.setDataPath("hdfs://localhost:4007/user/hive/warehouse/default");
        sinkRequest.setFileFormat(StandardCharsets.UTF_8.name());
        sinkRequest.setDataSeparator("|");
        streamSinkService.save(sinkRequest, OPERATOR);
        return sinkRequest;
    }

    /**
     * Creat kafka source info by inlong stream info.
     */
    public KafkaSourceRequest createKafkaSource(InlongStreamInfo streamInfo) {
        KafkaSourceRequest kafkaSourceRequest = new KafkaSourceRequest();
        kafkaSourceRequest.setInlongGroupId(streamInfo.getInlongGroupId());
        kafkaSourceRequest.setInlongStreamId(streamInfo.getInlongStreamId());
        kafkaSourceRequest.setGroupId("default");
        kafkaSourceRequest.setSerializationType("csv");
        kafkaSourceRequest.setSourceName("KAFKA");
        streamSourceService.save(kafkaSourceRequest, OPERATOR);
        return kafkaSourceRequest;
    }

    // @Test
    public void testCreateSortConfigInUpdateWorkflow() {
        InlongGroupInfo groupInfo = createInlongGroup("test20", MQType.PULSAR);
        groupInfo.setEnableZookeeper(InlongConstants.ENABLE_ZK);
        groupInfo.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_SUCCESSFUL.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);

        InlongStreamInfo streamInfo = createStreamInfo(groupInfo, "test_stream_info");
        createHiveSink(streamInfo);
        createKafkaSource(streamInfo);
        GroupResourceProcessForm form = new GroupResourceProcessForm();
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(GroupOperateType.SUSPEND);
        taskListenerFactory.acceptPlugin(new MockPlugin());

        WorkflowContext context = processService.start(ProcessName.SUSPEND_GROUP_PROCESS.name(), applicant, form);
        WorkflowResult result = WorkflowUtils.getResult(context);
        ProcessResponse response = result.getProcessInfo();
        Assertions.assertSame(response.getStatus(), ProcessStatus.COMPLETED);
        WorkflowProcess process = context.getProcess();
        WorkflowTask task = process.getTaskByName("StopSort");
        Assertions.assertTrue(task instanceof ServiceTask);
        Assertions.assertEquals(2, task.getNameToListenerMap().size());
        List<TaskEventListener> listeners = Lists.newArrayList(task.getNameToListenerMap().values());
        Assertions.assertEquals(2, listeners.size());
        ProcessForm currentProcessForm = context.getProcessForm();
        InlongGroupInfo curGroupRequest = ((GroupResourceProcessForm) currentProcessForm).getGroupInfo();
        Assertions.assertEquals(1, curGroupRequest.getExtList().size());
    }

}
