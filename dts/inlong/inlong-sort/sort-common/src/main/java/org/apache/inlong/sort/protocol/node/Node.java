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

package org.apache.inlong.sort.protocol.node;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.extract.FileSystemExtractNode;
import org.apache.inlong.sort.protocol.node.extract.HudiExtractNode;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.extract.MongoExtractNode;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.extract.OracleExtractNode;
import org.apache.inlong.sort.protocol.node.extract.PostgresExtractNode;
import org.apache.inlong.sort.protocol.node.extract.PulsarExtractNode;
import org.apache.inlong.sort.protocol.node.extract.RedisExtractNode;
import org.apache.inlong.sort.protocol.node.extract.SqlServerExtractNode;
import org.apache.inlong.sort.protocol.node.extract.TubeMQExtractNode;
import org.apache.inlong.sort.protocol.node.extract.DorisExtractNode;
import org.apache.inlong.sort.protocol.node.load.ClickHouseLoadNode;
import org.apache.inlong.sort.protocol.node.load.DorisLoadNode;
import org.apache.inlong.sort.protocol.node.load.ElasticsearchLoadNode;
import org.apache.inlong.sort.protocol.node.load.FileSystemLoadNode;
import org.apache.inlong.sort.protocol.node.load.GreenplumLoadNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.load.HiveLoadNode;
import org.apache.inlong.sort.protocol.node.load.HudiLoadNode;
import org.apache.inlong.sort.protocol.node.load.IcebergLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.load.KuduLoadNode;
import org.apache.inlong.sort.protocol.node.load.MySqlLoadNode;
import org.apache.inlong.sort.protocol.node.load.OracleLoadNode;
import org.apache.inlong.sort.protocol.node.load.PostgresLoadNode;
import org.apache.inlong.sort.protocol.node.load.RedisLoadNode;
import org.apache.inlong.sort.protocol.node.load.SqlServerLoadNode;
import org.apache.inlong.sort.protocol.node.load.StarRocksLoadNode;
import org.apache.inlong.sort.protocol.node.load.TDSQLPostgresLoadNode;
import org.apache.inlong.sort.protocol.node.transform.DistinctNode;
import org.apache.inlong.sort.protocol.node.transform.TransformNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Base class for extract node \ load node \ transform node
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MySqlExtractNode.class, name = "mysqlExtract"),
        @JsonSubTypes.Type(value = KafkaExtractNode.class, name = "kafkaExtract"),
        @JsonSubTypes.Type(value = PostgresExtractNode.class, name = "postgresExtract"),
        @JsonSubTypes.Type(value = FileSystemExtractNode.class, name = "fileSystemExtract"),
        @JsonSubTypes.Type(value = SqlServerExtractNode.class, name = "sqlserverExtract"),
        @JsonSubTypes.Type(value = PulsarExtractNode.class, name = "pulsarExtract"),
        @JsonSubTypes.Type(value = MongoExtractNode.class, name = "mongoExtract"),
        @JsonSubTypes.Type(value = OracleExtractNode.class, name = "oracleExtract"),
        @JsonSubTypes.Type(value = TubeMQExtractNode.class, name = "tubeMQExtract"),
        @JsonSubTypes.Type(value = RedisExtractNode.class, name = "redisExtract"),
        @JsonSubTypes.Type(value = DorisExtractNode.class, name = "dorisExtract"),
        @JsonSubTypes.Type(value = HudiExtractNode.class, name = "hudiExtract"),
        @JsonSubTypes.Type(value = TransformNode.class, name = "baseTransform"),
        @JsonSubTypes.Type(value = DistinctNode.class, name = "distinct"),
        @JsonSubTypes.Type(value = KafkaLoadNode.class, name = "kafkaLoad"),
        @JsonSubTypes.Type(value = HiveLoadNode.class, name = "hiveLoad"),
        @JsonSubTypes.Type(value = HbaseLoadNode.class, name = "hbaseLoad"),
        @JsonSubTypes.Type(value = PostgresLoadNode.class, name = "postgresLoad"),
        @JsonSubTypes.Type(value = FileSystemLoadNode.class, name = "fileSystemLoad"),
        @JsonSubTypes.Type(value = ClickHouseLoadNode.class, name = "clickHouseLoad"),
        @JsonSubTypes.Type(value = SqlServerLoadNode.class, name = "sqlserverLoad"),
        @JsonSubTypes.Type(value = TDSQLPostgresLoadNode.class, name = "tdsqlPostgresLoad"),
        @JsonSubTypes.Type(value = MySqlLoadNode.class, name = "mysqlLoad"),
        @JsonSubTypes.Type(value = IcebergLoadNode.class, name = "icebergLoad"),
        @JsonSubTypes.Type(value = ElasticsearchLoadNode.class, name = "elasticsearchLoad"),
        @JsonSubTypes.Type(value = OracleLoadNode.class, name = "oracleLoad"),
        @JsonSubTypes.Type(value = GreenplumLoadNode.class, name = "greenplumLoad"),
        @JsonSubTypes.Type(value = DorisLoadNode.class, name = "dorisLoad"),
        @JsonSubTypes.Type(value = HudiLoadNode.class, name = "hudiLoad"),
        @JsonSubTypes.Type(value = DorisLoadNode.class, name = "dorisLoad"),
        @JsonSubTypes.Type(value = StarRocksLoadNode.class, name = "starRocksLoad"),
        @JsonSubTypes.Type(value = RedisLoadNode.class, name = "redisLoad"),
        @JsonSubTypes.Type(value = KuduLoadNode.class, name = "kuduLoad"),
})
public interface Node {

    String getId();

    @JsonInclude(Include.NON_NULL)
    String getName();

    List<FieldInfo> getFields();

    @JsonInclude(Include.NON_NULL)
    default Map<String, String> getProperties() {
        return new TreeMap<>();
    }

    @JsonInclude(Include.NON_NULL)
    default Map<String, String> tableOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        if (getProperties() != null && !getProperties().isEmpty()) {
            options.putAll(getProperties());
        }
        return options;
    }

    String genTableName();

    @JsonInclude(Include.NON_NULL)
    default String getPrimaryKey() {
        return null;
    }

    @JsonInclude(Include.NON_NULL)
    default List<FieldInfo> getPartitionFields() {
        return null;
    }

}
