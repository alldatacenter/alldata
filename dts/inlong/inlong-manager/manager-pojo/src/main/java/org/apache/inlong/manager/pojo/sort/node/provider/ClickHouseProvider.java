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
import org.apache.inlong.manager.common.fieldtype.strategy.ClickHouseFieldTypeStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeMappingStrategy;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseSink;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.load.ClickHouseLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating ClickHouse load nodes.
 */
public class ClickHouseProvider implements LoadNodeProvider {

    private static final FieldTypeMappingStrategy FIELD_TYPE_MAPPING_STRATEGY = new ClickHouseFieldTypeStrategy();

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.CLICKHOUSE.equals(sinkType);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        ClickHouseSink streamSink = (ClickHouseSink) nodeInfo;
        Map<String, String> properties = parseProperties(streamSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(streamSink.getSinkFieldList(), streamSink.getSinkName(),
                FIELD_TYPE_MAPPING_STRATEGY);
        List<FieldRelation> fieldRelations = parseSinkFields(streamSink.getSinkFieldList(), constantFieldMap);
        return new ClickHouseLoadNode(
                streamSink.getSinkName(),
                streamSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                streamSink.getDbName() + "." + streamSink.getTableName(),
                streamSink.getJdbcUrl() + "/" + streamSink.getDbName(),
                streamSink.getUsername(),
                streamSink.getPassword(),
                streamSink.getPrimaryKey());
    }
}