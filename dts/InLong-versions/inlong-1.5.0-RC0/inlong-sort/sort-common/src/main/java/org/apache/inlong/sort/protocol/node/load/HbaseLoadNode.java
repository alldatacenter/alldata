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

package org.apache.inlong.sort.protocol.node.load;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.constant.HBaseConstant;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Hbase load node for generate hbase connector DDL
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("hbaseLoad")
@Data
@NoArgsConstructor
public class HbaseLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("tableName")
    private String tableName;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("zookeeperQuorum")
    private String zookeeperQuorum;

    @JsonProperty("rowKey")
    private String rowKey;

    @JsonProperty("sinkBufferFlushMaxSize")
    private String sinkBufferFlushMaxSize;

    @JsonProperty("zookeeperZnodeParent")
    private String zookeeperZnodeParent;

    @JsonProperty("sinkBufferFlushMaxRows")
    private String sinkBufferFlushMaxRows;

    @JsonProperty("sinkBufferFlushInterval")
    private String sinkBufferFlushInterval;

    @JsonCreator
    public HbaseLoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("namespace") String namespace,
            @JsonProperty("zookeeperQuorum") String zookeeperQuorum,
            @JsonProperty("rowKey") String rowKey,
            @JsonProperty("sinkBufferFlushMaxSize") String sinkBufferFlushMaxSize,
            @JsonProperty("zookeeperZnodeParent") String zookeeperZnodeParent,
            @JsonProperty("sinkBufferFlushMaxRows") String sinkBufferFlushMaxRows,
            @JsonProperty("sinkBufferFlushInterval") String sinkBufferFlushInterval) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.tableName = Preconditions.checkNotNull(tableName, "tableName of hbase is null");
        this.namespace = Preconditions.checkNotNull(namespace, "namespace of hbase is null");
        this.zookeeperQuorum = Preconditions.checkNotNull(zookeeperQuorum, "zookeeperQuorum of hbase is null");
        this.rowKey = Preconditions.checkNotNull(rowKey, "rowKey of hbase is null");
        this.sinkBufferFlushMaxSize = sinkBufferFlushMaxSize;
        this.zookeeperZnodeParent = zookeeperZnodeParent;
        this.sinkBufferFlushMaxRows = sinkBufferFlushMaxRows;
        this.sinkBufferFlushInterval = sinkBufferFlushInterval;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> map = super.tableOptions();
        map.put(HBaseConstant.CONNECTOR, HBaseConstant.HBASE_2);
        map.put(HBaseConstant.TABLE_NAME, namespace + ":" + tableName);
        map.put(HBaseConstant.ZOOKEEPER_QUORUM, zookeeperQuorum);
        if (StringUtils.isNotEmpty(sinkBufferFlushInterval)) {
            map.put(HBaseConstant.SINK_BUFFER_FLUSH_INTERVAL, sinkBufferFlushInterval);
        }
        if (StringUtils.isNotEmpty(zookeeperZnodeParent)) {
            map.put(HBaseConstant.ZOOKEEPER_ZNODE_PARENT, zookeeperZnodeParent);
        }
        if (StringUtils.isNotEmpty(sinkBufferFlushMaxRows)) {
            map.put(HBaseConstant.SINK_BUFFER_FLUSH_MAX_ROWS, sinkBufferFlushMaxRows);
        }
        if (StringUtils.isNotEmpty(sinkBufferFlushMaxSize)) {
            map.put(HBaseConstant.SINK_BUFFER_FLUSH_MAX_SIZE, sinkBufferFlushMaxSize);
        }
        return map;
    }

    @Override
    public String genTableName() {
        return this.tableName;
    }
}
