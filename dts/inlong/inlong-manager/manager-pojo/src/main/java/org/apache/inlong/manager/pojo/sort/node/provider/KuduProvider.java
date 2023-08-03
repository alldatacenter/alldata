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

package org.apache.inlong.manager.pojo.sort.node.provider;

import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.pojo.sink.kudu.KuduSink;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.load.KuduLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating Kudu load nodes.
 */
public class KuduProvider implements LoadNodeProvider {

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.KUDU.equals(sinkType);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        KuduSink kuduSink = (KuduSink) nodeInfo;
        Map<String, String> properties = parseProperties(kuduSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(kuduSink.getSinkFieldList(), kuduSink.getSinkName());
        List<FieldRelation> fieldRelations = parseSinkFields(kuduSink.getSinkFieldList(), constantFieldMap);

        return new KuduLoadNode(
                kuduSink.getSinkName(),
                kuduSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                kuduSink.getMasters(),
                kuduSink.getTableName(),
                kuduSink.getPartitionKey());
    }
}