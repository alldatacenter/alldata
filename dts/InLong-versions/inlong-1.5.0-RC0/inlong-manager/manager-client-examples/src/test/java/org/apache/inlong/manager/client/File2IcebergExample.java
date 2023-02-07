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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongGroupContext;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergColumnInfo;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergPartition;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergSink;
import org.apache.inlong.manager.pojo.source.file.FileSource;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.shiro.util.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test class for file to iceberg.
 */
@Slf4j
@Disabled
public class File2IcebergExample extends BaseExample {

    @Test
    public void testCreateGroupForIceberg() {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setWriteTimeout(10);
        configuration.setReadTimeout(10);
        configuration.setConnectTimeout(10);
        configuration.setTimeUnit(TimeUnit.SECONDS);
        configuration.setAuthentication(super.getInlongAuth());
        InlongClient inlongClient = InlongClient.create(super.getServiceUrl(), configuration);

        InlongGroupInfo groupInfo = super.createGroupInfo();
        try {
            InlongGroup group = inlongClient.forGroup(groupInfo);
            InlongStreamBuilder streamBuilder = group.createStream(createStreamInfo());
            streamBuilder.fields(createStreamFields());
            streamBuilder.source(createAgentFileSource());
            streamBuilder.sink(createIcebergSink());
            streamBuilder.initOrUpdate();
            // start group
            InlongGroupContext inlongGroupContext = group.init();
            Assert.notNull(inlongGroupContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStopGroup() {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setWriteTimeout(10);
        configuration.setReadTimeout(10);
        configuration.setConnectTimeout(10);
        configuration.setTimeUnit(TimeUnit.SECONDS);
        configuration.setAuthentication(super.getInlongAuth());
        InlongClient inlongClient = InlongClient.create(super.getServiceUrl(), configuration);
        InlongGroupInfo groupInfo = createGroupInfo();
        try {
            InlongGroup group = inlongClient.forGroup(groupInfo);
            InlongGroupContext groupContext = group.delete();
            Assert.notNull(groupContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FileSource createAgentFileSource() {
        FileSource fileSource = new FileSource();
        fileSource.setSourceName("{source.name}");
        fileSource.setAgentIp("{agent.ip}");
        fileSource.setPattern("/a/b/*.txt");
        fileSource.setTimeOffset("-1h");
        return fileSource;
    }

    private List<StreamField> createStreamFields() {
        List<StreamField> streamFieldList = Lists.newArrayList();
        streamFieldList.add(new StreamField(0, FieldType.STRING.toString(), "name", null, null));
        streamFieldList.add(new StreamField(1, FieldType.INT.toString(), "age", null, null));
        streamFieldList.add(new StreamField(2, FieldType.DECIMAL.toString(), "score", null, null));
        streamFieldList.add(new StreamField(3, FieldType.TIMESTAMP.toString(), "ts", null, null));
        return streamFieldList;
    }

    /**
     * Create Iceberg sink
     */
    public IcebergSink createIcebergSink() {
        IcebergSink sink = new IcebergSink();

        sink.setSinkName("{sink.name}");
        sink.setDbName("{db.name}");
        sink.setTableName("{table.name}");
        sink.setCatalogUri("thrift://{ip:port}");
        sink.setWarehouse("hdfs://{ip:port}/user/iceberg/warehouse/");

        final SinkField field1 = new SinkField(0, FieldType.INT.toString(), "age", FieldType.INT.toString(), "age");
        final SinkField field2 = new SinkField(1, FieldType.STRING.toString(), "name", FieldType.STRING.toString(),
                "name");
        final SinkField field3 = new SinkField(3, FieldType.DECIMAL.toString(), "score", FieldType.DECIMAL.toString(),
                "score");
        final SinkField field4 = new SinkField(3, FieldType.TIMESTAMP.toString(), "ts", FieldType.TIMESTAMP.toString(),
                "ts");

        // field ext param
        // field1: bucket partition example
        IcebergColumnInfo info1 = new IcebergColumnInfo();
        info1.setRequired(true);
        info1.setPartitionStrategy(IcebergPartition.BUCKET.toString());
        info1.setBucketNum(10);
        field1.setExtParams(JsonUtils.toJsonString(info1));

        // field3: decimal column example
        IcebergColumnInfo info3 = new IcebergColumnInfo();
        info3.setScale(5);
        info3.setPrecision(10); // scale must be less than or equal to precision
        field3.setExtParams(JsonUtils.toJsonString(info3));

        // field4: hour partition example
        IcebergColumnInfo info4 = new IcebergColumnInfo();
        info4.setPartitionStrategy(IcebergPartition.HOUR.toString());
        field4.setExtParams(JsonUtils.toJsonString(info4));

        List<SinkField> fields = new ArrayList<>();
        fields.add(field1);
        fields.add(field2);
        fields.add(field3);
        fields.add(field4);
        sink.setSinkFieldList(fields);
        return sink;
    }
}
