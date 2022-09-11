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
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sink.SinkField;
import org.apache.inlong.manager.common.pojo.sink.hive.HiveSinkRequest;
import org.apache.inlong.manager.common.pojo.source.kafka.KafkaSourceRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.form.GroupResourceProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.mocks.MockPlugin;
import org.apache.inlong.manager.service.sink.StreamSinkService;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.service.workflow.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowServiceImplTest;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;
import org.apache.inlong.manager.workflow.util.WorkflowBeanUtils;
import org.junit.Assert;
import org.junit.Before;
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

    @Before
    public void init() {
        subType = "DisableZkFor";
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
        sinkRequest.setDataSeparator("124");
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

    // There will be concurrency problems in the overall operation,This method temporarily fails the test
    // @Test
    public void testCreateSortConfigInCreateWorkflow() {
        InlongGroupInfo groupInfo = initGroupForm("PULSAR", "test21");
        groupInfo.setStatus(GroupStatus.CONFIG_SUCCESSFUL.getCode());
        groupInfo.setEnableZookeeper(0);
        groupService.update(groupInfo.genRequest(), OPERATOR);
        InlongStreamInfo streamInfo = createStreamInfo(groupInfo);
        createHiveSink(streamInfo);
        createKafkaSource(streamInfo);
        mockTaskListenerFactory();
        WorkflowContext context = processService.start(processName.name(), applicant, form);
        WorkflowResult result = WorkflowBeanUtils.result(context);
        ProcessResponse response = result.getProcessInfo();
        Assert.assertSame(response.getStatus(), ProcessStatus.COMPLETED);
        WorkflowProcess process = context.getProcess();
        WorkflowTask task = process.getTaskByName("initSort");
        Assert.assertTrue(task instanceof ServiceTask);
        Assert.assertEquals(1, task.getNameToListenerMap().size());

        List<TaskEventListener> listeners = Lists.newArrayList(task.getNameToListenerMap().values());
        Assert.assertTrue(listeners.get(0) instanceof CreateSortConfigListener);
        ProcessForm form = context.getProcessForm();
        InlongGroupInfo curGroupRequest = ((GroupResourceProcessForm) form).getGroupInfo();
        Assert.assertEquals(1, curGroupRequest.getExtList().size());
    }

    //    @Test
    public void testCreateSortConfigInUpdateWorkflow() {
        InlongGroupInfo groupInfo = initGroupForm("PULSAR", "test20");
        groupInfo.setEnableZookeeper(0);
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_SUCCESSFUL.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);

        InlongStreamInfo streamInfo = createStreamInfo(groupInfo);
        createHiveSink(streamInfo);
        createKafkaSource(streamInfo);
        GroupResourceProcessForm form = new GroupResourceProcessForm();
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(GroupOperateType.SUSPEND);
        taskListenerFactory.acceptPlugin(new MockPlugin());

        WorkflowContext context = processService.start(ProcessName.SUSPEND_GROUP_PROCESS.name(), applicant, form);
        WorkflowResult result = WorkflowBeanUtils.result(context);
        ProcessResponse response = result.getProcessInfo();
        Assert.assertSame(response.getStatus(), ProcessStatus.COMPLETED);
        WorkflowProcess process = context.getProcess();
        WorkflowTask task = process.getTaskByName("stopSort");
        Assert.assertTrue(task instanceof ServiceTask);
        Assert.assertEquals(2, task.getNameToListenerMap().size());
        List<TaskEventListener> listeners = Lists.newArrayList(task.getNameToListenerMap().values());
        Assert.assertTrue(listeners.get(1) instanceof CreateSortConfigListener);
        ProcessForm currentProcessForm = context.getProcessForm();
        InlongGroupInfo curGroupRequest = ((GroupResourceProcessForm) currentProcessForm).getGroupInfo();
        Assert.assertEquals(1, curGroupRequest.getExtList().size());
    }

}
