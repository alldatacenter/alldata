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

import org.apache.inlong.manager.common.consts.StreamType;
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeMappingStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.SQLServerFieldTypeStrategy;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerSink;
import org.apache.inlong.manager.pojo.sort.node.base.ExtractNodeProvider;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.source.sqlserver.SQLServerSource;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.extract.SqlServerExtractNode;
import org.apache.inlong.sort.protocol.node.load.SqlServerLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating SQLServer extract or load nodes.
 */
public class SQLServerProvider implements ExtractNodeProvider, LoadNodeProvider {

    private static final FieldTypeMappingStrategy FIELD_TYPE_MAPPING_STRATEGY = new SQLServerFieldTypeStrategy();

    @Override
    public Boolean accept(String streamType) {
        return StreamType.SQLSERVER.equals(streamType);
    }

    @Override
    public ExtractNode createExtractNode(StreamNode streamNodeInfo) {
        SQLServerSource source = (SQLServerSource) streamNodeInfo;
        List<FieldInfo> fieldInfos = parseStreamFieldInfos(source.getFieldList(), source.getSourceName(),
                FIELD_TYPE_MAPPING_STRATEGY);
        Map<String, String> properties = parseProperties(source.getProperties());

        return new SqlServerExtractNode(
                source.getSourceName(),
                source.getSourceName(),
                fieldInfos,
                null,
                properties,
                source.getPrimaryKey(),
                source.getHostname(),
                source.getPort(),
                source.getUsername(),
                source.getPassword(),
                source.getDatabase(),
                source.getSchemaName(),
                source.getTableName(),
                source.getServerTimezone());
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        SQLServerSink sqlServerSink = (SQLServerSink) nodeInfo;
        Map<String, String> properties = parseProperties(sqlServerSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(sqlServerSink.getSinkFieldList(), sqlServerSink.getSinkName(),
                FIELD_TYPE_MAPPING_STRATEGY);
        List<FieldRelation> fieldRelations = parseSinkFields(sqlServerSink.getSinkFieldList(), constantFieldMap);

        return new SqlServerLoadNode(
                sqlServerSink.getSinkName(),
                sqlServerSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                sqlServerSink.getJdbcUrl(),
                sqlServerSink.getUsername(),
                sqlServerSink.getPassword(),
                sqlServerSink.getSchemaName(),
                sqlServerSink.getTableName(),
                sqlServerSink.getPrimaryKey());
    }
}