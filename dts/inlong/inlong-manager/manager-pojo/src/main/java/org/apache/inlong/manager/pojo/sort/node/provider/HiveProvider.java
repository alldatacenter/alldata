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
import org.apache.inlong.manager.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.sort.util.FieldInfoUtils;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.load.HiveLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Provider for creating Hive load nodes.
 */
public class HiveProvider implements LoadNodeProvider {

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.HIVE.equals(sinkType);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        HiveSink hiveSink = (HiveSink) nodeInfo;
        Map<String, String> properties = parseProperties(hiveSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(hiveSink.getSinkFieldList(), hiveSink.getSinkName());
        List<FieldRelation> fieldRelations = parseSinkFields(hiveSink.getSinkFieldList(), constantFieldMap);
        List<FieldInfo> partitionFields = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(hiveSink.getPartitionFieldList())) {
            partitionFields = hiveSink.getPartitionFieldList().stream()
                    .map(partitionField -> new FieldInfo(partitionField.getFieldName(), hiveSink.getSinkName(),
                            FieldInfoUtils.convertFieldFormat(partitionField.getFieldType(),
                                    partitionField.getFieldFormat())))
                    .collect(Collectors.toList());
        }
        return new HiveLoadNode(
                hiveSink.getSinkName(),
                hiveSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                Lists.newArrayList(),
                null,
                null,
                properties,
                null,
                hiveSink.getDbName(),
                hiveSink.getTableName(),
                hiveSink.getHiveConfDir(),
                hiveSink.getHiveVersion(),
                null,
                partitionFields);
    }
}