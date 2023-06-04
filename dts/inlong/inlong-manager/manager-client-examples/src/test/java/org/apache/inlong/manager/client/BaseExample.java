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

package org.apache.inlong.manager.client;

import lombok.Data;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base example class for client.
 */
@Data
public class BaseExample {

    // Service url of the inlong manager
    private String serviceUrl = "127.0.0.1:8083";
    // Inlong user && passwd
    private DefaultAuthentication inlongAuth = new DefaultAuthentication("admin", "inlong");
    // Inlong group ID
    private String groupId = "{group.id}";
    // Inlong stream ID
    private String streamId = "{stream.id}";
    // Flink cluster url
    private String flinkUrl = "{flink.cluster.url}";
    // Pulsar cluster admin url
    private String pulsarAdminUrl = "{pulsar.admin.url}";
    // Pulsar cluster service url
    private String pulsarServiceUrl = "{pulsar.service.url}";
    // Pulsar tenant
    private String tenant = "{pulsar.tenant}";
    // Pulsar tenant
    private String namespace = "{pulsar.namespace}";
    // Pulsar topic
    private String topic = "{pulsar.topic}";

    /**
     * Create inlong group info
     */
    public InlongGroupInfo createGroupInfo() {
        InlongPulsarInfo pulsarInfo = new InlongPulsarInfo();
        pulsarInfo.setInlongGroupId(groupId);
        pulsarInfo.setInCharges("admin");

        // pulsar conf
        pulsarInfo.setTenant(tenant);
        pulsarInfo.setMqResource(namespace);

        // set enable zk, create resource, lightweight mode, and cluster tag
        pulsarInfo.setEnableZookeeper(InlongConstants.DISABLE_ZK);
        pulsarInfo.setEnableCreateResource(InlongConstants.ENABLE_CREATE_RESOURCE);
        pulsarInfo.setLightweight(InlongConstants.STANDARD_MODE);
        pulsarInfo.setInlongClusterTag("default_cluster");

        pulsarInfo.setDailyRecords(10000000);
        pulsarInfo.setDailyStorage(10000);
        pulsarInfo.setPeakRecords(100000);
        pulsarInfo.setMaxLength(10000);

        // flink conf
        FlinkSortConf sortConf = new FlinkSortConf();
        sortConf.setServiceUrl(flinkUrl);
        Map<String, String> map = new HashMap<>(16);
        sortConf.setProperties(map);
        pulsarInfo.setSortConf(sortConf);

        return pulsarInfo;
    }

    /**
     * Create inlong stream info
     */
    public InlongStreamInfo createStreamInfo() {
        InlongStreamInfo streamInfo = new InlongStreamInfo();
        streamInfo.setName(this.getStreamId());
        streamInfo.setInlongStreamId(this.getStreamId());
        streamInfo.setDataEncoding(StandardCharsets.UTF_8.toString());
        streamInfo.setDataSeparator("|");
        // if you need strictly order for data, set to 1
        streamInfo.setSyncSend(InlongConstants.SYNC_SEND);
        streamInfo.setMqResource(this.getTopic());
        return streamInfo;
    }

    /**
     * Create hive sink
     */
    public HiveSink createHiveSink() {
        HiveSink hiveSink = new HiveSink();
        hiveSink.setDbName("{db.name}");
        hiveSink.setJdbcUrl("jdbc:hive2://{ip:port}");
        hiveSink.setAuthentication(new DefaultAuthentication("hive", "hive"));
        hiveSink.setDataEncoding(StandardCharsets.UTF_8.toString());
        hiveSink.setFileFormat(FileFormat.TextFile.name());
        hiveSink.setDataSeparator("|");
        hiveSink.setDataPath("hdfs://{ip:port}/usr/hive/warehouse/{db.name}");
        hiveSink.setHiveConfDir("{hive.conf.dir}");

        List<SinkField> fields = new ArrayList<>();
        SinkField field1 = new SinkField(0, FieldType.INT.toString(), "age", FieldType.INT.toString(), "age");
        SinkField field2 = new SinkField(1, FieldType.STRING.toString(), "name", FieldType.STRING.toString(), "name");
        fields.add(field1);
        fields.add(field2);
        hiveSink.setSinkFieldList(fields);
        hiveSink.setTableName("{table.name}");
        hiveSink.setSinkName("{hive.sink.name}");
        return hiveSink;
    }

}
