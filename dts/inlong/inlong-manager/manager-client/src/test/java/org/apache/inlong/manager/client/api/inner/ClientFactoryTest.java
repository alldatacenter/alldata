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

package org.apache.inlong.manager.client.api.inner;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.common.constant.ProtocolType;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.impl.InlongClientImpl;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.DataNodeClient;
import org.apache.inlong.manager.client.api.inner.client.InlongClusterClient;
import org.apache.inlong.manager.client.api.inner.client.InlongConsumeClient;
import org.apache.inlong.manager.client.api.inner.client.InlongGroupClient;
import org.apache.inlong.manager.client.api.inner.client.InlongStreamClient;
import org.apache.inlong.manager.client.api.inner.client.StreamSinkClient;
import org.apache.inlong.manager.client.api.inner.client.StreamSourceClient;
import org.apache.inlong.manager.client.api.inner.client.UserClient;
import org.apache.inlong.manager.client.api.inner.client.WorkflowClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.auth.DefaultAuthentication;
import org.apache.inlong.manager.common.auth.TokenAuthentication;
import org.apache.inlong.manager.common.consts.DataNodeType;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.ClusterType;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.cluster.BindTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterInfo;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterNodeResponse;
import org.apache.inlong.manager.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagRequest;
import org.apache.inlong.manager.pojo.cluster.ClusterTagResponse;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterInfo;
import org.apache.inlong.manager.pojo.cluster.pulsar.PulsarClusterRequest;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.group.InlongGroupBriefInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupCountResponse;
import org.apache.inlong.manager.pojo.group.InlongGroupExtInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupResetRequest;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarRequest;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarTopicInfo;
import org.apache.inlong.manager.pojo.node.DataNodeInfo;
import org.apache.inlong.manager.pojo.node.DataNodePageRequest;
import org.apache.inlong.manager.pojo.node.hive.HiveDataNodeInfo;
import org.apache.inlong.manager.pojo.node.hive.HiveDataNodeRequest;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.pojo.sink.hbase.HBaseSink;
import org.apache.inlong.manager.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergSink;
import org.apache.inlong.manager.pojo.sink.kafka.KafkaSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSink;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLSink;
import org.apache.inlong.manager.pojo.sort.FlinkSortConf;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.autopush.AutoPushSource;
import org.apache.inlong.manager.pojo.source.file.FileSource;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSource;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.apache.inlong.manager.common.consts.InlongConstants.STATEMENT_TYPE_JSON;

/**
 * Unit test for {@link ClientFactory}.
 */
@Slf4j
class ClientFactoryTest {

    private static final int SERVICE_PORT = 8085;
    static ClientFactory clientFactory;
    private static WireMockServer wireMockServer;
    private static InlongGroupClient groupClient;
    private static InlongStreamClient streamClient;
    private static StreamSourceClient sourceClient;
    private static StreamSinkClient sinkClient;
    private static InlongClusterClient clusterClient;
    private static DataNodeClient dataNodeClient;
    private static UserClient userClient;
    private static WorkflowClient workflowClient;
    private static InlongConsumeClient consumeClient;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(options().port(SERVICE_PORT));
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());

        String serviceUrl = "127.0.0.1:" + SERVICE_PORT;
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setAuthentication(new DefaultAuthentication("admin", "inlong"));
        InlongClientImpl inlongClient = new InlongClientImpl(serviceUrl, configuration);
        clientFactory = ClientUtils.getClientFactory(inlongClient.getConfiguration());

        groupClient = clientFactory.getGroupClient();
        streamClient = clientFactory.getStreamClient();
        sourceClient = clientFactory.getSourceClient();
        sinkClient = clientFactory.getSinkClient();
        streamClient = clientFactory.getStreamClient();
        clusterClient = clientFactory.getClusterClient();
        dataNodeClient = clientFactory.getDataNodeClient();
        userClient = clientFactory.getUserClient();
        workflowClient = clientFactory.getWorkflowClient();
        consumeClient = clientFactory.getConsumeClient();
    }

    @AfterAll
    static void teardown() {
        wireMockServer.stop();
    }

    @Test
    void testGroupExist() {
        stubFor(
                get(urlMatching("/inlong/manager/api/group/exist/123.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(true)))));
        Boolean groupExists = groupClient.isGroupExists("123");
        Assertions.assertTrue(groupExists);
    }

    @Test
    void testGetGroupInfo() {
        FlinkSortConf flinkSortConf = new FlinkSortConf();
        flinkSortConf.setAuthentication(new TokenAuthentication());
        InlongPulsarInfo pulsarInfo = new InlongPulsarInfo();
        pulsarInfo.setId(1);
        pulsarInfo.setInlongGroupId("1");
        pulsarInfo.setMqType("PULSAR");
        pulsarInfo.setEnableCreateResource(1);
        pulsarInfo.setExtList(
                Lists.newArrayList(InlongGroupExtInfo.builder()
                        .id(1)
                        .inlongGroupId("1")
                        .keyName("keyName")
                        .keyValue("keyValue")
                        .build()));
        pulsarInfo.setSortConf(flinkSortConf);

        stubFor(
                get(urlMatching("/inlong/manager/api/group/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(pulsarInfo)))));

        InlongGroupInfo groupInfo = groupClient.getGroupInfo("1");
        Assertions.assertTrue(groupInfo instanceof InlongPulsarInfo);
        Assertions.assertEquals(JsonUtils.toJsonString(pulsarInfo), JsonUtils.toJsonString(groupInfo));
    }

    @Test
    void testListGroup4AutoPushSource() {
        List<InlongGroupBriefInfo> groupBriefInfos = Lists.newArrayList(
                InlongGroupBriefInfo.builder()
                        .id(1)
                        .inlongGroupId("1")
                        .name("name")
                        .streamSources(
                                Lists.newArrayList(
                                        AutoPushSource.builder()
                                                .id(22)
                                                .inlongGroupId("1")
                                                .inlongStreamId("2")
                                                .sourceType(SourceType.AUTO_PUSH)
                                                .dataProxyGroup("111")
                                                .build()))
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/group/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(groupBriefInfos))))));

        PageResult<InlongGroupBriefInfo> pageInfo = groupClient.listGroups("keyword", 1, 1, 10);
        Assertions.assertEquals(JsonUtils.toJsonString(groupBriefInfos),
                JsonUtils.toJsonString(pageInfo.getList()));
    }

    @Test
    void testListGroup4BinlogSource() {
        List<InlongGroupBriefInfo> groupBriefInfos = Lists.newArrayList(
                InlongGroupBriefInfo.builder()
                        .id(1)
                        .inlongGroupId("1")
                        .name("name")
                        .streamSources(
                                Lists.newArrayList(
                                        MySQLBinlogSource.builder()
                                                .id(22)
                                                .inlongGroupId("1")
                                                .inlongStreamId("2")
                                                .sourceType(SourceType.MYSQL_BINLOG)
                                                .status(1)
                                                .user("root")
                                                .password("pwd")
                                                .databaseWhiteList("")
                                                .build()))
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/group/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(groupBriefInfos))))));

        PageResult<InlongGroupBriefInfo> pageInfo = groupClient.listGroups("keyword", 1, 1, 10);
        Assertions.assertEquals(JsonUtils.toJsonString(groupBriefInfos),
                JsonUtils.toJsonString(pageInfo.getList()));
    }

    @Test
    void testListGroup4FileSource() {
        List<InlongGroupBriefInfo> groupBriefInfos = Lists.newArrayList(
                InlongGroupBriefInfo.builder()
                        .id(1)
                        .inlongGroupId("1")
                        .name("name")
                        .inCharges("admin")
                        .status(1)
                        .createTime(new Date())
                        .modifyTime(new Date())
                        .streamSources(
                                Lists.newArrayList(
                                        FileSource.builder()
                                                .id(22)
                                                .inlongGroupId("1")
                                                .inlongStreamId("2")
                                                .sourceType(SourceType.FILE)
                                                .status(1)
                                                .agentIp("127.0.0.1")
                                                .pattern("pattern")
                                                .build()))
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/group/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(groupBriefInfos))))));

        PageResult<InlongGroupBriefInfo> pageInfo = groupClient.listGroups("keyword", 1, 1, 10);
        Assertions.assertEquals(JsonUtils.toJsonString(groupBriefInfos),
                JsonUtils.toJsonString(pageInfo.getList()));
    }

    @Test
    void testListGroup4KafkaSource() {
        List<InlongGroupBriefInfo> groupBriefInfos = Lists.newArrayList(
                InlongGroupBriefInfo.builder()
                        .id(1)
                        .inlongGroupId("1")
                        .streamSources(
                                Lists.newArrayList(
                                        KafkaSource.builder()
                                                .id(22)
                                                .inlongGroupId("1")
                                                .inlongStreamId("2")
                                                .sourceType(SourceType.KAFKA)
                                                .dataNodeName("dataNodeName")
                                                .version(1)
                                                .createTime(new Date())
                                                .modifyTime(new Date())
                                                .topic("topic")
                                                .groupId("111")
                                                .bootstrapServers("bootstrapServers")
                                                .recordSpeedLimit("recordSpeedLimit")
                                                .primaryKey("primaryKey")
                                                .build()))
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/group/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(groupBriefInfos))))));

        PageResult<InlongGroupBriefInfo> pageInfo = groupClient.listGroups("keyword", 1, 1, 10);
        Assertions.assertEquals(JsonUtils.toJsonString(groupBriefInfos),
                JsonUtils.toJsonString(pageInfo.getList()));
    }

    @Test
    void testListGroup4AllSource() {
        ArrayList<StreamSource> streamSources = Lists.newArrayList(
                AutoPushSource.builder()
                        .id(22)
                        .inlongGroupId("1")
                        .inlongStreamId("2")
                        .sourceType(SourceType.AUTO_PUSH)
                        .version(1)
                        .build(),

                MySQLBinlogSource.builder()
                        .id(22)
                        .inlongGroupId("1")
                        .inlongStreamId("2")
                        .sourceType(SourceType.MYSQL_BINLOG)
                        .user("root")
                        .password("pwd")
                        .hostname("localhost")
                        .includeSchema("false")
                        .databaseWhiteList("")
                        .tableWhiteList("")
                        .build(),

                FileSource.builder()
                        .id(22)
                        .inlongGroupId("1")
                        .inlongStreamId("2")
                        .version(1)
                        .agentIp("127.0.0.1")
                        .pattern("pattern")
                        .timeOffset("timeOffset")
                        .build(),

                KafkaSource.builder()
                        .id(22)
                        .inlongGroupId("1")
                        .inlongStreamId("2")
                        .sourceType(SourceType.KAFKA)
                        .sourceName("source name")
                        .serializationType("csv")
                        .dataNodeName("dataNodeName")
                        .topic("topic")
                        .groupId("111")
                        .bootstrapServers("bootstrapServers")
                        .recordSpeedLimit("recordSpeedLimit")
                        .primaryKey("primaryKey")
                        .build());
        List<InlongGroupBriefInfo> groupBriefInfos = Lists.newArrayList(
                InlongGroupBriefInfo.builder()
                        .id(1)
                        .inlongGroupId("1")
                        .name("name")
                        .inCharges("admin")
                        .streamSources(streamSources)
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/group/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(groupBriefInfos))))));

        PageResult<InlongGroupBriefInfo> pageInfo = groupClient.listGroups("keyword", 1, 1, 10);
        Assertions.assertEquals(JsonUtils.toJsonString(groupBriefInfos),
                JsonUtils.toJsonString(pageInfo.getList()));
    }

    @Test
    void testListGroup4NotExist() {
        stubFor(
                post(urlMatching("/inlong/manager/api/group/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.fail("Inlong group does not exist/no operation authority")))));

        PageResult<InlongGroupBriefInfo> pageInfo = groupClient.listGroups("keyword", 1, 1, 10);
        Assertions.assertNull(pageInfo);
    }

    @Test
    void testCreateGroup() {
        stubFor(
                post(urlMatching("/inlong/manager/api/group/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success("1111")))));

        String groupId = groupClient.createGroup(new InlongPulsarRequest());
        Assertions.assertEquals("1111", groupId);
    }

    @Test
    void testUpdateGroup() {
        stubFor(
                post(urlMatching("/inlong/manager/api/group/update.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success("1111")))));

        Pair<String, String> updateGroup = groupClient.updateGroup(new InlongPulsarRequest());
        Assertions.assertEquals("1111", updateGroup.getKey());
        Assertions.assertTrue(StringUtils.isBlank(updateGroup.getValue()));
    }

    @Test
    void testCountGroupByUser() {
        InlongGroupCountResponse expected = new InlongGroupCountResponse();
        expected.setRejectCount(102400L);
        expected.setTotalCount(834781232L);
        expected.setWaitApproveCount(34524L);
        expected.setWaitAssignCount(45678L);
        stubFor(
                get(urlMatching("/inlong/manager/api/group/countByStatus.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(expected)))));
        InlongGroupCountResponse actual = groupClient.countGroupByUser();
        Assertions.assertEquals(expected.getRejectCount(), actual.getRejectCount());
        Assertions.assertEquals(expected.getTotalCount(), actual.getTotalCount());
        Assertions.assertEquals(expected.getWaitApproveCount(), actual.getWaitApproveCount());
        Assertions.assertEquals(expected.getWaitAssignCount(), actual.getWaitAssignCount());
    }

    @Test
    void getTopic() {
        InlongPulsarTopicInfo expected = new InlongPulsarTopicInfo();
        expected.setInlongGroupId("1");
        expected.setNamespace("testTopic");
        PulsarClusterInfo clusterInfo = new PulsarClusterInfo();
        clusterInfo.setUrl("pulsar://127.0.0.1:6650");
        clusterInfo.setAdminUrl("http://127.0.0.1:8080");
        expected.setClusterInfos(Collections.singletonList(clusterInfo));
        expected.setTopics(new ArrayList<>());
        stubFor(
                get(urlMatching("/inlong/manager/api/group/getTopic/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(expected)))));

        InlongPulsarTopicInfo actual = (InlongPulsarTopicInfo) groupClient.getTopic("1");
        Assertions.assertEquals(expected.getInlongGroupId(), actual.getInlongGroupId());
        Assertions.assertEquals(expected.getMqType(), actual.getMqType());
        Assertions.assertEquals(expected.getTopics().size(), actual.getTopics().size());
        Assertions.assertEquals(expected.getClusterInfos().size(), actual.getClusterInfos().size());
    }

    @Test
    void testCreateStream() {
        stubFor(
                post(urlMatching("/inlong/manager/api/stream/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(11)))));

        Integer groupId = streamClient.createStreamInfo(new InlongStreamInfo());
        Assertions.assertEquals(11, groupId);
    }

    @Test
    void testStreamExist() {
        stubFor(
                get(urlMatching("/inlong/manager/api/stream/exist/123/11.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(true)))));

        InlongStreamInfo streamInfo = new InlongStreamInfo();
        streamInfo.setInlongGroupId("123");
        streamInfo.setInlongStreamId("11");
        Boolean groupExists = streamClient.isStreamExists(streamInfo);

        Assertions.assertTrue(groupExists);
    }

    @Test
    void testGetStream() {
        InlongStreamInfo streamInfo = new InlongStreamInfo();
        streamInfo.setId(1);
        streamInfo.setInlongGroupId("123");
        streamInfo.setInlongStreamId("11");
        streamInfo.setName("name");
        streamInfo.setFieldList(
                Lists.newArrayList(
                        StreamField.builder()
                                .id(1)
                                .inlongGroupId("123")
                                .fieldType("string")
                                .build(),
                        StreamField.builder()
                                .id(2)
                                .inlongGroupId("123")
                                .inlongGroupId("11")
                                .isMetaField(1)
                                .build()));

        stubFor(
                get(urlMatching("/inlong/manager/api/stream/get.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(streamInfo)))));

        InlongStreamInfo streamInfoResult = streamClient.getStreamInfo("123", "11");
        Assertions.assertNotNull(streamInfoResult);
    }

    @Test
    void testGetStream4NotExist() {
        stubFor(
                get(urlMatching("/inlong/manager/api/stream/get.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.fail("Inlong stream does not exist/no operation permission")))));

        InlongStreamInfo inlongStreamInfo = streamClient.getStreamInfo("123", "11");
        Assertions.assertNull(inlongStreamInfo);
    }

    @Test
    void testListStream4AllSink() {
        InlongStreamInfo streamInfo = new InlongStreamInfo();
        streamInfo.setId(1);
        streamInfo.setInlongGroupId("11");
        streamInfo.setInlongStreamId("11");
        streamInfo.setFieldList(
                Lists.newArrayList(
                        StreamField.builder()
                                .id(1)
                                .inlongGroupId("123")
                                .inlongGroupId("11")
                                .build(),
                        StreamField.builder()
                                .id(2)
                                .isMetaField(1)
                                .fieldFormat("yyyy-MM-dd HH:mm:ss")
                                .build()));

        ArrayList<StreamSource> sourceList = Lists.newArrayList(
                AutoPushSource.builder()
                        .id(1)
                        .inlongStreamId("11")
                        .inlongGroupId("11")
                        .sourceType(SourceType.AUTO_PUSH)
                        .createTime(new Date())

                        .dataProxyGroup("111")
                        .build(),
                MySQLBinlogSource.builder()
                        .id(2)
                        .sourceType(SourceType.MYSQL_BINLOG)
                        .user("user")
                        .password("pwd")
                        .build(),
                FileSource.builder()
                        .id(3)
                        .sourceType(SourceType.FILE)
                        .agentIp("127.0.0.1")
                        .pattern("pattern")
                        .build(),
                KafkaSource.builder()
                        .id(4)
                        .sourceType(SourceType.KAFKA)
                        .autoOffsetReset("11")
                        .bootstrapServers("127.0.0.1")
                        .build());

        ArrayList<StreamSink> sinkList = Lists.newArrayList(
                HiveSink.builder()
                        .sinkType(SinkType.HIVE)
                        .id(1)
                        .jdbcUrl("127.0.0.1")
                        .build(),
                ClickHouseSink.builder()
                        .sinkType(SinkType.CLICKHOUSE)
                        .id(2)
                        .flushInterval(11)
                        .build(),
                IcebergSink.builder()
                        .sinkType(SinkType.ICEBERG)
                        .id(3)
                        .dataPath("hdfs://aabb")
                        .build(),
                KafkaSink.builder()
                        .sinkType(SinkType.KAFKA)
                        .id(4)
                        .bootstrapServers("127.0.0.1")
                        .build());

        streamInfo.setSourceList(sourceList);
        streamInfo.setSinkList(sinkList);

        stubFor(
                post(urlMatching("/inlong/manager/api/stream/listAll.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(new PageResult<>(Lists.newArrayList(streamInfo)))))));

        List<InlongStreamInfo> streamInfos = streamClient.listStreamInfo("11");
        Assertions.assertEquals(JsonUtils.toJsonString(streamInfo), JsonUtils.toJsonString(streamInfos.get(0)));
    }

    @Test
    void testListSink4AllType() {
        List<StreamSink> sinkList = Lists.newArrayList(
                ClickHouseSink.builder()
                        .id(1)
                        .sinkType(SinkType.CLICKHOUSE)
                        .jdbcUrl("127.0.0.1")
                        .partitionStrategy("BALANCE")
                        .partitionFields("partitionFields")
                        .build(),
                ElasticsearchSink.builder()
                        .id(2)
                        .sinkType(SinkType.ELASTICSEARCH)
                        .hosts("http://127.0.0.1:9200")
                        .flushInterval(2)
                        .build(),
                HBaseSink.builder()
                        .id(3)
                        .sinkType(SinkType.HBASE)
                        .tableName("tableName")
                        .rowKey("rowKey")
                        .build(),
                HiveSink.builder()
                        .id(4)
                        .sinkType(SinkType.HIVE)
                        .dataPath("hdfs://ip:port/user/hive/warehouse/test.db")
                        .hiveVersion("hiveVersion")
                        .build(),
                IcebergSink.builder()
                        .id(5)
                        .sinkType(SinkType.ICEBERG)
                        .partitionType("H-hour")
                        .build(),
                KafkaSink.builder()
                        .id(6)
                        .sinkType(SinkType.KAFKA)
                        .topicName("test")
                        .partitionNum("6")
                        .build(),
                PostgreSQLSink.builder()
                        .id(7)
                        .sinkType(SinkType.POSTGRESQL)
                        .primaryKey("test")
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/sink/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(new PageResult<>(Lists.newArrayList(sinkList)))))));

        List<StreamSink> sinks = sinkClient.listSinks("11", "11");
        Assertions.assertEquals(JsonUtils.toJsonString(sinkList), JsonUtils.toJsonString(sinks));
    }

    @Test
    void testListSink4AllTypeShouldThrowException() {
        stubFor(
                post(urlMatching("/inlong/manager/api/sink/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.fail("groupId should not empty")))));

        RuntimeException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> sinkClient.listSinks("", "11"));
        Assertions.assertTrue(exception.getMessage().contains("groupId should not empty"));
    }

    @Test
    void testResetGroup() {
        stubFor(
                post(urlMatching("/inlong/manager/api/group/reset.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(true)))));

        boolean isReset = groupClient.resetGroup(new InlongGroupResetRequest());
        Assertions.assertTrue(isReset);
    }

    @Test
    void testSaveCluster() {
        stubFor(
                post(urlMatching("/inlong/manager/api/cluster/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(1)))));
        ClusterRequest request = new PulsarClusterRequest();
        request.setName("pulsar");
        request.setClusterTags("test_cluster");
        Integer clusterIndex = clusterClient.saveCluster(request);
        Assertions.assertEquals(1, (int) clusterIndex);
    }

    @Test
    void testGetCluster() {
        ClusterInfo cluster = PulsarClusterInfo.builder()
                .id(1)
                .name("test_cluster")
                .url("127.0.0.1")
                .clusterTags("test_cluster_tag")
                .type(ClusterType.PULSAR)
                .adminUrl("http://127.0.0.1:8080")
                .build();

        stubFor(
                get(urlMatching("/inlong/manager/api/cluster/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(cluster)))));

        ClusterInfo clusterInfo = clusterClient.get(1);
        Assertions.assertEquals(1, clusterInfo.getId());
        Assertions.assertTrue(clusterInfo instanceof PulsarClusterInfo);
    }

    @Test
    void testGetMysqlSinkInfo() {
        StreamSink streamSink = MySQLSink.builder()
                // mysql field
                .jdbcUrl("127.0.0.1:3306")
                .username("test")
                .password("pwd")
                .tableName("tableName")
                .primaryKey("id")
                // streamSink field
                .id(1)
                .inlongGroupId("1")
                .inlongStreamId("1")
                .sinkType(SinkType.MYSQL)
                .sinkName("mysql_test")
                // streamNode field
                .preNodes(new HashSet<>())
                .postNodes(new HashSet<>())
                .fieldList(
                        Lists.newArrayList(StreamField.builder()
                                .fieldName("id")
                                .fieldType("int")
                                .build()))
                .build();

        stubFor(
                get(urlMatching("/inlong/manager/api/sink/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(streamSink)))));

        StreamSink sinkInfo = sinkClient.getSinkInfo(1);
        Assertions.assertEquals(1, sinkInfo.getId());
        Assertions.assertTrue(sinkInfo instanceof MySQLSink);
    }

    @Test
    void testSaveClusterTag() {
        stubFor(
                post(urlMatching("/inlong/manager/api/cluster/tag/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(1)))));
        ClusterTagRequest request = new ClusterTagRequest();
        request.setClusterTag("test_cluster");
        Integer tagId = clusterClient.saveTag(request);
        Assertions.assertEquals(1, tagId);
    }

    @Test
    void testGetClusterTag() {
        ClusterTagResponse tagResponse = ClusterTagResponse.builder()
                .id(1)
                .clusterTag("test_cluster")
                .creator("admin")
                .inCharges("admin")
                .build();
        stubFor(
                get(urlMatching("/inlong/manager/api/cluster/tag/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(tagResponse)))));
        ClusterTagResponse clusterTagInfo = clusterClient.getTag(1);
        Assertions.assertNotNull(clusterTagInfo);
    }

    @Test
    void testBindTag() {
        stubFor(
                post(urlMatching("/inlong/manager/api/cluster/bindTag.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(true)))));
        BindTagRequest request = new BindTagRequest();
        request.setClusterTag("test_cluster_tag");
        Boolean isBind = clusterClient.bindTag(request);
        Assertions.assertTrue(isBind);
    }

    @Test
    void testSaveNode() {
        stubFor(
                post(urlMatching("/inlong/manager/api/cluster/node/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(1)))));
        ClusterNodeRequest request = new ClusterNodeRequest();
        request.setType(ClusterType.PULSAR);
        Integer nodeId = clusterClient.saveNode(request);
        Assertions.assertEquals(1, nodeId);
    }

    @Test
    void testGetClusterNode() {
        ClusterNodeResponse response = ClusterNodeResponse.builder()
                .id(1)
                .type(ClusterType.DATAPROXY)
                .ip("127.0.0.1")
                .port(46801)
                .build();
        stubFor(
                get(urlMatching("/inlong/manager/api/cluster/node/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(response)))));
        ClusterNodeResponse clientNode = clusterClient.getNode(1);
        Assertions.assertEquals(1, clientNode.getId());
    }

    @Test
    void testListClusterNode() {
        ClusterNodeResponse response = ClusterNodeResponse.builder()
                .id(5)
                .type(ClusterType.DATAPROXY)
                .parentId(1)
                .ip("127.0.0.1")
                .port(46801)
                .protocolType(ProtocolType.HTTP)
                .build();
        List<ClusterNodeResponse> responses = new ArrayList<>();
        responses.add(response);
        stubFor(
                get(urlMatching("/inlong/manager/api/cluster/node/listByGroupId.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(responses)))));
        List<ClusterNodeResponse> clusterNode = clusterClient.listNode(
                "1", ClusterType.DATAPROXY, ProtocolType.HTTP);
        Assertions.assertEquals(1, clusterNode.size());
    }

    @Test
    void testGetStreamSourceById() {
        StreamSource streamSource = MySQLBinlogSource.builder()
                .id(1)
                .inlongGroupId("test_group")
                .inlongStreamId("test_stream")
                .sourceType(SourceType.MYSQL_BINLOG)
                .sourceName("test_source")
                .agentIp("127.0.0.1")
                .inlongClusterName("test_cluster")
                .user("root")
                .password("pwd")
                .hostname("127.0.0.1")
                .port(3306)
                .build();

        stubFor(
                get(urlMatching("/inlong/manager/api/source/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(streamSource)))));
        StreamSource sourceInfo = sourceClient.get(1);
        Assertions.assertEquals(1, sourceInfo.getId());
        Assertions.assertTrue(sourceInfo instanceof MySQLBinlogSource);
    }

    @Test
    void testSaveDataNode() {
        stubFor(
                post(urlMatching("/inlong/manager/api/node/save.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(1)))));
        HiveDataNodeRequest request = new HiveDataNodeRequest();
        request.setName("test_hive_node");
        Integer nodeId = dataNodeClient.save(request);
        Assertions.assertEquals(1, nodeId);
    }

    @Test
    void testGetDataNode() {
        HiveDataNodeInfo dataNodeInfo = HiveDataNodeInfo.builder()
                .id(1)
                .name("test_node")
                .type(DataNodeType.HIVE)
                .build();
        stubFor(
                get(urlMatching("/inlong/manager/api/node/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(dataNodeInfo)))));
        DataNodeInfo nodeInfo = dataNodeClient.get(1);
        Assertions.assertEquals(1, nodeInfo.getId());
    }

    @Test
    void testListDataNode() {
        List<DataNodeInfo> nodeResponses = Lists.newArrayList(
                HiveDataNodeInfo.builder()
                        .id(1)
                        .type(DataNodeType.HIVE)
                        .build());

        stubFor(
                post(urlMatching("/inlong/manager/api/node/list.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(Response.success(new PageResult<>(nodeResponses))))));

        DataNodePageRequest pageRequest = new DataNodePageRequest();
        PageResult<DataNodeInfo> pageInfo = dataNodeClient.list(pageRequest);
        Assertions.assertEquals(JsonUtils.toJsonString(pageInfo.getList()), JsonUtils.toJsonString(nodeResponses));
    }

    @Test
    void testUpdateDataNode() {
        stubFor(
                post(urlMatching("/inlong/manager/api/node/update.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(true)))));

        HiveDataNodeRequest request = new HiveDataNodeRequest();
        request.setId(1);
        request.setName("test_hive_node");
        Boolean isUpdate = dataNodeClient.update(request);
        Assertions.assertTrue(isUpdate);
    }

    @Test
    void testDeleteDataNode() {
        stubFor(
                delete(urlMatching("/inlong/manager/api/node/delete/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(true)))));
        Boolean isUpdate = dataNodeClient.delete(1);
        Assertions.assertTrue(isUpdate);
    }

    @Test
    void testRegisterUser() {
        stubFor(
                post(urlMatching("/inlong/manager/api/user/register.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(1)))));
        UserRequest request = new UserRequest();
        request.setName("test_user");
        request.setPassword("test_pwd");
        Integer userId = userClient.register(request);
        Assertions.assertEquals(1, userId);
    }

    @Test
    void testGetUserById() {
        UserInfo userInfo = UserInfo.builder()
                .id(1)
                .name("test_user")
                .password("test_pwd")
                .build();

        stubFor(
                get(urlMatching("/inlong/manager/api/user/get/1.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(userInfo)))));
        UserInfo info = userClient.getById(1);
        Assertions.assertEquals(info.getId(), 1);
    }

    @Test
    void testUpdateUser() {
        stubFor(
                post(urlMatching("/inlong/manager/api/user/update.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(1)))));
        UserRequest request = new UserRequest();
        request.setId(1);
        request.setName("test_user");
        request.setPassword("test_pwd");
        request.setNewPassword("test_new_pwd");
        request.setAccountType(UserTypeEnum.ADMIN.getCode());
        Integer userId = userClient.update(request);
        Assertions.assertEquals(userId, 1);
    }

    @Test
    void testDeleteUser() {
        stubFor(
                delete(urlMatching("/inlong/manager/api/user/delete.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(true)))));
        Boolean isDelete = userClient.delete(1);
        Assertions.assertTrue(isDelete);
    }

    @Test
    void testParseStreamFields() {
        List<StreamField> streamFieldList = Lists.newArrayList(
                StreamField.builder()
                        .fieldName("test_name")
                        .fieldType("string")
                        .build());
        stubFor(
                post(urlMatching("/inlong/manager/api/stream/parseFields.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(Lists.newArrayList(streamFieldList))))));

        List<StreamField> responseList = streamClient.parseFields(STATEMENT_TYPE_JSON, "{\"test_name\":\"string\"}");
        Assertions.assertEquals(JsonUtils.toJsonString(responseList), JsonUtils.toJsonString(streamFieldList));

    }

    @Test
    void testParseSinkFields() {
        List<SinkField> sinkFieldList = Lists.newArrayList(
                SinkField.builder()
                        .fieldName("test_name")
                        .fieldType("string")
                        .build());
        stubFor(
                post(urlMatching("/inlong/manager/api/sink/parseFields.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(Lists.newArrayList(sinkFieldList))))));

        List<SinkField> responseList = sinkClient.parseFields(STATEMENT_TYPE_JSON, "{\"test_name\":\"string\"}");
        Assertions.assertEquals(JsonUtils.toJsonString(responseList), JsonUtils.toJsonString(sinkFieldList));

    }

}
