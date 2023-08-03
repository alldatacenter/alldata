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
import org.apache.inlong.manager.pojo.sink.hdfs.HDFSSink;
import org.apache.inlong.manager.pojo.sort.node.base.LoadNodeProvider;
import org.apache.inlong.manager.pojo.sort.util.FieldInfoUtils;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.manager.pojo.stream.StreamNode;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.load.FileSystemLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Provider for creating HDFS load nodes.
 */
public class HDFSProvider implements LoadNodeProvider {

    @Override
    public Boolean accept(String sinkType) {
        return SinkType.HDFS.equals(sinkType);
    }

    @Override
    public LoadNode createLoadNode(StreamNode nodeInfo, Map<String, StreamField> constantFieldMap) {
        HDFSSink hdfsSink = (HDFSSink) nodeInfo;
        Map<String, String> properties = parseProperties(hdfsSink.getProperties());
        List<FieldInfo> fieldInfos = parseSinkFieldInfos(hdfsSink.getSinkFieldList(), hdfsSink.getSinkName());
        List<FieldRelation> fieldRelations = parseSinkFields(hdfsSink.getSinkFieldList(), constantFieldMap);
        List<FieldInfo> partitionFields = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(hdfsSink.getPartitionFieldList())) {
            partitionFields = hdfsSink.getPartitionFieldList().stream()
                    .map(partitionField -> new FieldInfo(partitionField.getFieldName(), hdfsSink.getSinkName(),
                            FieldInfoUtils.convertFieldFormat(partitionField.getFieldType(),
                                    partitionField.getFieldFormat())))
                    .collect(Collectors.toList());
        }

        return new FileSystemLoadNode(
                hdfsSink.getSinkName(),
                hdfsSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                Lists.newArrayList(),
                hdfsSink.getDataPath(),
                hdfsSink.getFileFormat(),
                null,
                properties,
                partitionFields,
                hdfsSink.getServerTimeZone());
    }
}