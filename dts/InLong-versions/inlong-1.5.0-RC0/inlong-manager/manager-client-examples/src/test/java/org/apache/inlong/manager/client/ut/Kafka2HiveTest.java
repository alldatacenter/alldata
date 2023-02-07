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

package org.apache.inlong.manager.client.ut;

import com.google.common.collect.Lists;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongGroupContext;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.common.enums.DataFormat;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.workflow.EventLogResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.inlong.manager.common.enums.ProcessStatus.PROCESSING;

class Kafka2HiveTest extends BaseTest {

    @BeforeAll
    static void createStub() {
        stubFor(
                get(urlMatching(MANAGER_URL_PREFIX + "/group/exist/test_group009.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(false)))));

        stubFor(
                post(urlMatching(MANAGER_URL_PREFIX + "/group/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success("test_group009")))));

        stubFor(
                get(urlMatching(MANAGER_URL_PREFIX + "/stream/exist/test_group009/test_stream009.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(false)))));

        stubFor(
                get(urlMatching(MANAGER_URL_PREFIX + "/stream/exist/test_group009/test_stream009.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(false)))));

        stubFor(
                post(urlMatching(MANAGER_URL_PREFIX + "/stream/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(6)))));

        stubFor(
                post(urlMatching(MANAGER_URL_PREFIX + "/source/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(6)))));

        stubFor(
                post(urlMatching(MANAGER_URL_PREFIX + "/sink/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(6)))));

        WorkflowResult initWorkflowResult = new WorkflowResult();
        initWorkflowResult.setProcessInfo(
                ProcessResponse.builder()
                        .id(12)
                        .name(ProcessName.APPLY_GROUP_PROCESS.name())
                        .displayName(ProcessName.APPLY_GROUP_PROCESS.getDisplayName())
                        .type("Apply-Group")
                        .applicant("admin")
                        .status(PROCESSING)
                        .startTime(new Date())
                        .formData(JsonUtils.parseTree(
                                "{\"formName\":\"ApplyGroupProcessForm\",\"groupInfo\":{\"mqType\":\"PULSAR\",\"id\":6,"
                                        + "\"inlongGroupId\":\"test_group009\",\"name\":null,\"description\":null,"
                                        + "\"mqResource\":\"test_namespace\",\"enableZookeeper\":0,"
                                        + "\"enableCreateResource\":1,\"lightweight\":1,"
                                        + "\"inlongClusterTag\":\"default_cluster\",\"dailyRecords\":10000000,"
                                        + "\"dailyStorage\":10000,\"peakRecords\":100000,\"maxLength\":10000,"
                                        + "\"inCharges\":\"test_inCharges,admin\",\"followers\":null,\"status\":101,"
                                        + "\"creator\":\"admin\",\"modifier\":\"admin\","
                                        + "\"createTime\":\"2022-06-06 09:59:10\","
                                        + "\"modifyTime\":\"2022-06-06 02:24:50\",\"extList\":[],\"tenant\":null,"
                                        + "\"adminUrl\":null,\"serviceUrl\":null,\"queueModule\":\"PARALLEL\","
                                        + "\"partitionNum\":3,\"ensemble\":3,\"writeQuorum\":3,\"ackQuorum\":2,"
                                        + "\"ttl\":24,\"ttlUnit\":\"hours\",\"retentionTime\":72,"
                                        + "\"retentionTimeUnit\":\"hours\",\"retentionSize\":-1,"
                                        + "\"retentionSizeUnit\":\"MB\"},\"streamInfoList\":[{\"id\":6,"
                                        + "\"inlongGroupId\":\"test_group009\",\"inlongStreamId\":\"test_stream009\","
                                        + "\"name\":\"test_stream009\",\"sinkList\":[{\"id\":6,"
                                        + "\"inlongGroupId\":\"test_group009\",\"inlongStreamId\":\"test_stream009\","
                                        + "\"sinkType\":\"HIVE\",\"sinkName\":\"{hive.sink.name}\",\"clusterId\":null,"
                                        + "\"clusterUrl\":null}],\"modifyTime\":\"2022-06-06 02:11:03\"}]}"))
                        .build());
        initWorkflowResult.setNewTasks(
                Lists.newArrayList(
                        TaskResponse.builder()
                                .id(12)
                                .type("UserTask")
                                .processId(12)
                                .processName(ProcessName.APPLY_GROUP_PROCESS.name())
                                .processDisplayName(ProcessName.APPLY_GROUP_PROCESS.getDisplayName())
                                .name("ut_admin")
                                .displayName("SystemAdmin")
                                .applicant("admin")
                                .approvers(Lists.newArrayList("admin"))
                                .status(TaskStatus.PENDING)
                                .startTime(new Date())
                                .build()));
        stubFor(
                post(urlMatching(MANAGER_URL_PREFIX + "/group/startProcess/test_group009.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(initWorkflowResult)))));

        WorkflowResult startWorkflowResult = new WorkflowResult();
        startWorkflowResult.setProcessInfo(
                ProcessResponse.builder()
                        .id(12)
                        .name("APPLY_GROUP_PROCESS")
                        .displayName("Apply Group")
                        .type("Apply-Group")
                        .applicant("admin")
                        .status(ProcessStatus.COMPLETED)
                        .startTime(new Date())
                        .endTime(new Date())
                        .formData("{\"formName\":\"ApplyGroupProcessForm\",\"groupInfo\":{\"mqType\":\"PULSAR\","
                                + "\"id\":8,\"inlongGroupId\":\"test_group011\",\"name\":null,\"description\":null,"
                                + "\"mqResource\":\"test_namespace\",\"enableZookeeper\":0,\"enableCreateResource\":1,"
                                + "\"lightweight\":1,\"inlongClusterTag\":\"default_cluster\","
                                + "\"dailyRecords\":10000000,\"dailyStorage\":10000,\"peakRecords\":100000,"
                                + "\"maxLength\":10000,\"inCharges\":\"test_inCharges,admin\",\"followers\":null,"
                                + "\"status\":101,\"creator\":\"admin\",\"modifier\":\"admin\","
                                + "\"createTime\":\"2022-06-06 16:36:35\",\"modifyTime\":\"2022-06-06 08:37:04\","
                                + "\"extList\":[],\"tenant\":null,\"adminUrl\":null,\"serviceUrl\":null,"
                                + "\"queueModule\":\"PARALLEL\",\"partitionNum\":3,\"ensemble\":3,\"writeQuorum\":3,"
                                + "\"ackQuorum\":2,\"ttl\":24,\"ttlUnit\":\"hours\",\"retentionTime\":72,"
                                + "\"retentionTimeUnit\":\"hours\",\"retentionSize\":-1,\"retentionSizeUnit\":\"MB\"},"
                                + "\"streamInfoList\":[{\"id\":8,\"inlongGroupId\":\"test_group011\","
                                + "\"inlongStreamId\":\"test_stream011\",\"name\":\"test_stream011\","
                                + "\"sinkList\":[{\"id\":8,\"inlongGroupId\":\"test_group011\","
                                + "\"inlongStreamId\":\"test_stream011\",\"sinkType\":\"HIVE\","
                                + "\"sinkName\":\"{hive.sink.name}\",\"clusterId\":null,\"clusterUrl\":null}],"
                                + "\"modifyTime\":\"2022-06-06 08:36:38\"}]}")
                        .build());
        startWorkflowResult.setNewTasks(new ArrayList<>());
        stubFor(
                post(urlMatching(MANAGER_URL_PREFIX + "/workflow/approve/12.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(startWorkflowResult)))));

        InlongPulsarInfo pulsarInfo = new InlongPulsarInfo();
        pulsarInfo.setId(8);
        pulsarInfo.setInlongGroupId("test_group009");
        pulsarInfo.setMqResource("test_namespace");
        pulsarInfo.setEnableZookeeper(0);
        pulsarInfo.setEnableCreateResource(1);
        pulsarInfo.setLightweight(1);
        pulsarInfo.setInlongClusterTag("default_cluster");
        pulsarInfo.setInCharges("test_inCharges,admin");
        pulsarInfo.setStatus(130);
        pulsarInfo.setCreator("admin");
        pulsarInfo.setModifier("admin");
        pulsarInfo.setQueueModule("PARALLEL");
        pulsarInfo.setPartitionNum(3);
        pulsarInfo.setEnsemble(3);
        pulsarInfo.setWriteQuorum(3);
        pulsarInfo.setTtl(24);
        pulsarInfo.setRetentionTime(72);
        pulsarInfo.setRetentionSize(-1);

        stubFor(
                get(urlMatching(MANAGER_URL_PREFIX + "/group/get/test_group009.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(pulsarInfo)))));

        InlongStreamInfo streamInfo = new InlongStreamInfo();
        streamInfo.setId(8);
        streamInfo.setInlongGroupId(GROUP_ID);
        streamInfo.setInlongStreamId(STREAM_ID);
        streamInfo.setMqResource("test_topic");
        streamInfo.setSyncSend(1);
        streamInfo.setStatus(130);
        streamInfo.setCreator("admin");
        streamInfo.setModifier("admin");
        streamInfo.setFieldList(createStreamFields());

        ArrayList<StreamSource> kafkaSources = Lists.newArrayList(
                KafkaSource.builder()
                        .id(6)
                        .topic(TOPIC)
                        .bootstrapServers("{kafka.bootstrap}")
                        .inlongGroupId(GROUP_ID)
                        .inlongStreamId(STREAM_ID)
                        .sourceType("KAFKA")
                        .sourceName("{kafka.source.name}")
                        .serializationType("json")
                        .version(1)
                        .status(110)
                        .creator("admin")
                        .modifier("admin")
                        .createTime(new Date())
                        .modifyTime(new Date())
                        .build());

        ArrayList<StreamSink> hiveSinks = Lists.newArrayList(
                HiveSink.builder()
                        .id(6)
                        .inlongStreamId(STREAM_ID)
                        .inlongGroupId(GROUP_ID)
                        .jdbcUrl("jdbc:hive2://{ip:port}")
                        .dbName("test_db")
                        .tableName("test_table")
                        .dataPath("hdfs://{ip:port}/usr/hive/warehouse/{db.name}")
                        .fileFormat("TextFile")
                        .dataEncoding("UTF-8")
                        .dataSeparator("|")
                        .sinkType("HIVE")
                        .sinkName("sink_name")
                        .enableCreateResource(1)
                        .status(110)
                        .creator("admin")
                        .modifier("admin")
                        .dataFormat(DataFormat.NONE)
                        .sinkFieldList(Lists.newArrayList(
                                SinkField.builder()
                                        .id(17)
                                        .fieldName("age")
                                        .fieldType("INT")
                                        .fieldComment("age")
                                        .sourceFieldName("age")
                                        .sourceFieldType("INT")
                                        .build(),
                                SinkField.builder()
                                        .id(18)
                                        .fieldName("name")
                                        .fieldType("STRING")
                                        .fieldComment("name")
                                        .sourceFieldName("name")
                                        .sourceFieldType("STRING")
                                        .build()))
                        .build());
        streamInfo.setSourceList(kafkaSources);
        streamInfo.setSinkList(hiveSinks);

        Response<PageResult<InlongStreamInfo>> fullStreamResponsePage = Response.success(
                new PageResult<>(Lists.newArrayList(streamInfo)));
        stubFor(
                post(urlMatching(MANAGER_URL_PREFIX + "/stream/listAll.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(fullStreamResponsePage))));

        EventLogResponse eventLogView1 = EventLogResponse.builder()
                .id(38)
                .processId(12)
                .processName(ProcessName.CREATE_GROUP_RESOURCE.toString())
                .processDisplayName(ProcessName.CREATE_GROUP_RESOURCE.getDisplayName())
                .inlongGroupId(GROUP_ID)
                .taskId(12)
                .elementName("InitSort")
                .elementDisplayName("Group-InitSort")
                .eventType("ProcessEvent")
                .event("FAIL")
                .listener("InitGroupFailedListener")
                .status(-1)
                .ip("127.0.0.1")
                .build();
        EventLogResponse eventLogView2 = EventLogResponse.builder()
                .id(39)
                .processId(12)
                .processName(ProcessName.CREATE_GROUP_RESOURCE.toString())
                .processDisplayName(ProcessName.CREATE_GROUP_RESOURCE.getDisplayName())
                .inlongGroupId(GROUP_ID)
                .taskId(12)
                .elementName("InitSort")
                .elementDisplayName("Group-InitSort")
                .eventType("TaskEvent")
                .event("COMPLETE")
                .listener("InitGroupListener")
                .ip("127.0.0.1")
                .build();
        stubFor(
                get(urlMatching(MANAGER_URL_PREFIX + "/workflow/event/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(
                                        Lists.newArrayList(eventLogView1, eventLogView2)))))));

        stubFor(
                get(urlMatching(MANAGER_URL_PREFIX + "/stream/config/log/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>())))));
    }

    private static KafkaSource createKafkaSource() {
        KafkaSource kafkaSource = new KafkaSource();
        kafkaSource.setBootstrapServers("127.0.0.1");
        kafkaSource.setTopic("test_topic");
        kafkaSource.setSourceName("kafka_source_name");
        kafkaSource.setSerializationType(DataFormat.JSON.getName());
        return kafkaSource;
    }

    private static List<StreamField> createStreamFields() {
        return Lists.newArrayList(
                new StreamField(0, FieldType.STRING.toString(), "name", null, null),
                new StreamField(1, FieldType.INT.toString(), "age", null, null));
    }

    @Test
    void testCreateGroupForHive() {
        Assertions.assertDoesNotThrow(() -> {
            InlongGroup group = inlongClient.forGroup(groupInfo);
            InlongStreamBuilder streamBuilder = group.createStream(createStreamInfo());
            streamBuilder.fields(createStreamFields());
            streamBuilder.source(createKafkaSource());
            streamBuilder.sink(createHiveSink());
            streamBuilder.initOrUpdate();
            // start group
            InlongGroupContext inlongGroupContext = group.init();
            Assertions.assertNotNull(inlongGroupContext);
        });

    }
}
