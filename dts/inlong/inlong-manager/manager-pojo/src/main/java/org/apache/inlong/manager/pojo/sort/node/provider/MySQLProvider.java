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
import org.apache.inlong.manager.common.fieldtype.strategy.FieldTypeMappingStrategy;
import org.apache.inlong.manager.common.fieldtype.strategy.MySQLFieldTypeStrategy;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSinkDTO;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.load.MySqlLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * The Provider for creating MySQL load nodes.
 */
public class MySQLProvider implements LoadNodeProvider {

    private static final FieldTypeMappingStrategy FIELD_TYPE_MAPPING_STRATEGY = new MySQLFieldTypeStrategy();

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.MYSQL.equals(sinkType);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        MySQLSink mysqlSink = (MySQLSink) nodeInfo;
        Map<String, String> properties = parseProperties(mysqlSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(mysqlSink.getSinkFieldList(), mysqlSink.getSinkName(),
                FIELD_TYPE_MAPPING_STRATEGY);
        List<FieldRelation> fieldRelations = parseSinkFields(mysqlSink.getSinkFieldList(), constantFieldMap);

        return new MySqlLoadNode(
                mysqlSink.getSinkName(),
                mysqlSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                Lists.newArrayList(),
                null,
                null,
                properties,
                MySQLSinkDTO.setDbNameToUrl(mysqlSink.getJdbcUrl(), mysqlSink.getDatabaseName()),
                mysqlSink.getUsername(),
                mysqlSink.getPassword(),
                mysqlSink.getTableName(),
                mysqlSink.getPrimaryKey());
    }
}