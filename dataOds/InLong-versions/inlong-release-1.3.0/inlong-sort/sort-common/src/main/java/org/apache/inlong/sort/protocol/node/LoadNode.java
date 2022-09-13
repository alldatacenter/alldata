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

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.load.ClickHouseLoadNode;
import org.apache.inlong.sort.protocol.node.load.DorisLoadNode;
import org.apache.inlong.sort.protocol.node.load.ElasticsearchLoadNode;
import org.apache.inlong.sort.protocol.node.load.DLCIcebergLoadNode;
import org.apache.inlong.sort.protocol.node.load.FileSystemLoadNode;
import org.apache.inlong.sort.protocol.node.load.GreenplumLoadNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.load.HiveLoadNode;
import org.apache.inlong.sort.protocol.node.load.IcebergLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.load.MySqlLoadNode;
import org.apache.inlong.sort.protocol.node.load.OracleLoadNode;
import org.apache.inlong.sort.protocol.node.load.PostgresLoadNode;
import org.apache.inlong.sort.protocol.node.load.SqlServerLoadNode;
import org.apache.inlong.sort.protocol.node.load.TDSQLPostgresLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * node for inserting data to external system
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = KafkaLoadNode.class, name = "kafkaLoad"),
        @JsonSubTypes.Type(value = HiveLoadNode.class, name = "hiveLoad"),
        @JsonSubTypes.Type(value = HbaseLoadNode.class, name = "hbaseLoad"),
        @JsonSubTypes.Type(value = FileSystemLoadNode.class, name = "fileSystemLoad"),
        @JsonSubTypes.Type(value = PostgresLoadNode.class, name = "postgresLoad"),
        @JsonSubTypes.Type(value = ClickHouseLoadNode.class, name = "clickHouseLoad"),
        @JsonSubTypes.Type(value = SqlServerLoadNode.class, name = "sqlserverLoad"),
        @JsonSubTypes.Type(value = TDSQLPostgresLoadNode.class, name = "tdsqlPostgresLoad"),
        @JsonSubTypes.Type(value = MySqlLoadNode.class, name = "mysqlLoad"),
        @JsonSubTypes.Type(value = IcebergLoadNode.class, name = "icebergLoad"),
        @JsonSubTypes.Type(value = ElasticsearchLoadNode.class, name = "elasticsearchLoad"),
        @JsonSubTypes.Type(value = OracleLoadNode.class, name = "oracleLoad"),
        @JsonSubTypes.Type(value = GreenplumLoadNode.class, name = "greenplumLoad"),
        @JsonSubTypes.Type(value = DLCIcebergLoadNode.class, name = "dlcIcebergLoad"),
        @JsonSubTypes.Type(value = DorisLoadNode.class, name = "dorisLoad")
})
@NoArgsConstructor
@Data
public abstract class LoadNode implements Node {

    @JsonProperty("id")
    private String id;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("name")
    private String name;
    @JsonProperty("fields")
    private List<FieldInfo> fields;
    @JsonProperty("fieldRelations")
    private List<FieldRelation> fieldRelations;
    @Nullable
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("sinkParallelism")
    private Integer sinkParallelism;
    @JsonProperty("filters")
    @JsonInclude(Include.NON_NULL)
    private List<FilterFunction> filters;
    @JsonProperty("filterStrategy")
    @JsonInclude(Include.NON_NULL)
    private FilterStrategy filterStrategy;
    @Nullable
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("properties")
    private Map<String, String> properties;

    @JsonCreator
    public LoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @Nullable @JsonProperty("properties") Map<String, String> properties) {
        this.id = Preconditions.checkNotNull(id, "id is null");
        this.name = name;
        this.fields = Preconditions.checkNotNull(fields, "fields is null");
        Preconditions.checkState(!fields.isEmpty(), "fields is empty");
        this.fieldRelations = Preconditions.checkNotNull(fieldRelations,
                "fieldRelations is null");
        Preconditions.checkState(!fieldRelations.isEmpty(), "fieldRelations is empty");
        this.filters = filters;
        this.filterStrategy = filterStrategy;
        this.sinkParallelism = sinkParallelism;
        this.properties = properties;
    }
}
