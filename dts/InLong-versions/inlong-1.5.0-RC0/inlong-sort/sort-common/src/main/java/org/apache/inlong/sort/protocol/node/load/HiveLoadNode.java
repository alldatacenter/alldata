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
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.formats.common.LocalZonedTimestampFormatInfo;
import org.apache.inlong.sort.formats.common.TimestampFormatInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.enums.FilterStrategy;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * hive load node with flink connectors
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("hiveLoad")
@Data
@NoArgsConstructor
public class HiveLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -4547768154621816459L;

    private static final String trigger = "sink.partition-commit.trigger";
    private static final String timestampPattern = "partition.time-extractor.timestamp-pattern";
    private static final String delay = "sink.partition-commit.delay";
    private static final String policyKind = "sink.partition-commit.policy.kind";
    private static final String HIVE_VERSION = "3.1.3";

    @JsonProperty("tableName")
    @Nonnull
    private String tableName;

    @JsonProperty("catalogName")
    private String catalogName;

    @JsonProperty("database")
    @Nonnull
    private String database;

    /**
     * hive conf directory in which contains hive-site.xml(vital)
     */
    @JsonProperty("hiveConfDir")
    private String hiveConfDir;

    @JsonProperty("hiveVersion")
    private String hiveVersion;

    @JsonProperty("hadoopConfDir")
    private String hadoopConfDir;

    @JsonProperty("partitionFields")
    private List<FieldInfo> partitionFields;

    @JsonCreator
    public HiveLoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @JsonProperty("filterStrategy") FilterStrategy filterStrategy,
            @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("catalogName") String catalogName,
            @JsonProperty("database") String database,
            @JsonProperty("tableName") String tableName,
            @JsonProperty("hiveConfDir") String hiveConfDir,
            @JsonProperty("hiveVersion") String hiveVersion,
            @JsonProperty("hadoopConfDir") String hadoopConfDir,
            @JsonProperty("partitionFields") List<FieldInfo> partitionFields) {
        super(id, name, fields, fieldRelations, filters, filterStrategy, sinkParallelism, properties);
        this.database = Preconditions.checkNotNull(database, "database of hive is null");
        this.tableName = Preconditions.checkNotNull(tableName, "table of hive is null");
        this.hiveConfDir = hiveConfDir;
        this.hiveVersion = null == hiveVersion ? HIVE_VERSION : hiveVersion;
        this.catalogName = catalogName;
        this.hadoopConfDir = hadoopConfDir;
        this.partitionFields = partitionFields;
        handleTimestampField();
    }

    /**
     * Dealing with problems caused by time precision in hive
     * Hive connector requires the time precision of timestamp and localzoned timestamp  must be 9
     */
    private void handleTimestampField() {
        getFields().forEach(f -> {
            if (f.getFormatInfo() instanceof TimestampFormatInfo) {
                ((TimestampFormatInfo) f.getFormatInfo()).setPrecision(9);
            }
            if (f.getFormatInfo() instanceof LocalZonedTimestampFormatInfo) {
                ((LocalZonedTimestampFormatInfo) f.getFormatInfo()).setPrecision(9);
            }
        });
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> map = super.tableOptions();
        map.put("connector", "hive");
        map.put("default-database", database);
        if (null != hiveVersion) {
            map.put("hive-version", hiveVersion);
        }
        if (null != hadoopConfDir) {
            map.put("hadoop-conf-dir", hadoopConfDir);
        }
        if (null != hiveConfDir) {
            map.put("hive-conf-dir", hiveConfDir);
        }
        if (null != partitionFields) {
            Map<String, String> properties = super.getProperties();
            if (null == properties || !properties.containsKey(trigger)) {
                map.put(trigger, "process-time");
            }
            if (null == properties || !properties.containsKey(timestampPattern)) {
                map.put(timestampPattern, "yyyy-MM-dd");
            }
            if (null == properties || !properties.containsKey(delay)) {
                map.put(delay, "10s");
            }
            if (null == properties || !properties.containsKey(policyKind)) {
                map.put(policyKind, "metastore,success-file");
            }
        }
        return map;
    }

    @Override
    public List<FieldInfo> getPartitionFields() {
        return partitionFields;
    }

    @Override
    public String genTableName() {
        return tableName;
    }

}
