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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.common.auth.DefaultAuthentication;
import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.enums.FileFormat;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.pulsar.InlongPulsarInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.pojo.sort.FlinkSortConf;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class BaseTest {

    // Inlong group ID
    public static final String GROUP_ID = "test_group009";
    // Inlong stream ID
    public static final String STREAM_ID = "test_stream009";
    // Flink cluster url
    public static final String FLINK_URL = "127.0.0.1";
    // Pulsar cluster admin url
    public static final String PULSAR_ADMIN_URL = "127.0.0.1";
    // Pulsar cluster service url
    public static final String PULSAR_SERVICE_URL = "127.0.0.1";
    // Pulsar tenant
    public static final String TENANT = "tenant";
    // Pulsar tenant
    public static final String NAMESPACE = "test_namespace";
    // Pulsar topic
    public static final String TOPIC = "test_topic";
    public static final String IN_CHARGES = "test_inCharges,admin";
    public static final String MANAGER_URL_PREFIX = "/inlong/manager/api";
    private static final int SERVICE_PORT = 8184;
    // Manager web url
    public static final String SERVICE_URL = "127.0.0.1:" + SERVICE_PORT;
    // Inlong user && passwd
    public static DefaultAuthentication inlongAuth = new DefaultAuthentication("admin", "inlong");
    public static WireMockServer wireMockServer;
    public static InlongGroupInfo groupInfo;
    public static InlongClient inlongClient;

    @BeforeAll
    static void setup() {
        // create mock server
        wireMockServer = new WireMockServer(options().port(SERVICE_PORT));
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());

        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setWriteTimeout(1000);
        configuration.setReadTimeout(1000);
        configuration.setConnectTimeout(1000);
        configuration.setTimeUnit(TimeUnit.SECONDS);
        configuration.setAuthentication(inlongAuth);

        inlongClient = InlongClient.create(SERVICE_URL, configuration);
        groupInfo = createGroupInfo();
    }

    @AfterAll
    static void teardown() {
        wireMockServer.stop();
    }

    /**
     * Create inlong group info
     */
    public static InlongGroupInfo createGroupInfo() {
        InlongPulsarInfo pulsarInfo = new InlongPulsarInfo();
        pulsarInfo.setInlongGroupId(GROUP_ID);
        pulsarInfo.setInCharges(IN_CHARGES);

        // pulsar conf
        pulsarInfo.setTenant(TENANT);
        pulsarInfo.setMqResource(NAMESPACE);

        // set enable zk, create resource, lightweight mode, and cluster tag
        pulsarInfo.setEnableZookeeper(InlongConstants.DISABLE_ZK);
        pulsarInfo.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        pulsarInfo.setLightweight(InlongConstants.LIGHTWEIGHT_MODE);
        pulsarInfo.setInlongClusterTag("default_cluster");

        pulsarInfo.setDailyRecords(10000000);
        pulsarInfo.setDailyStorage(10000);
        pulsarInfo.setPeakRecords(100000);
        pulsarInfo.setMaxLength(10000);

        // flink conf
        FlinkSortConf sortConf = new FlinkSortConf();
        sortConf.setServiceUrl(FLINK_URL);
        Map<String, String> map = new HashMap<>(16);
        sortConf.setProperties(map);
        pulsarInfo.setSortConf(sortConf);

        return pulsarInfo;
    }

    /**
     * Create hive sink
     */
    protected static HiveSink createHiveSink() {
        HiveSink hiveSink = new HiveSink();
        hiveSink.setDbName("{db.name}");
        hiveSink.setJdbcUrl("jdbc:hive2://{ip:port}");
        hiveSink.setAuthentication(new DefaultAuthentication("hive", "hive"));
        hiveSink.setDataEncoding(StandardCharsets.UTF_8.toString());
        hiveSink.setFileFormat(FileFormat.TextFile.name());
        hiveSink.setDataSeparator("|");
        hiveSink.setDataPath("hdfs://{ip:port}/usr/hive/warehouse/{db.name}");
        hiveSink.setSinkFieldList(Lists.newArrayList(
                new SinkField(0, FieldType.INT.toString(), "age", FieldType.INT.toString(), "age"),
                new SinkField(1, FieldType.STRING.toString(), "name", FieldType.STRING.toString(), "name")));

        hiveSink.setTableName("{table.name}");
        hiveSink.setSinkName("{hive.sink.name}");
        return hiveSink;
    }

    /**
     * Create inlong stream info
     */
    protected InlongStreamInfo createStreamInfo() {
        InlongStreamInfo streamInfo = new InlongStreamInfo();
        streamInfo.setInlongStreamId(STREAM_ID);
        streamInfo.setName(STREAM_ID);
        streamInfo.setDataEncoding(StandardCharsets.UTF_8.toString());
        streamInfo.setDataSeparator("|");
        // if you need strictly order for data, set to 1
        streamInfo.setSyncSend(InlongConstants.SYNC_SEND);
        streamInfo.setMqResource(TOPIC);
        return streamInfo;
    }
}
