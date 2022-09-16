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
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.InlongMetric;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FilterFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@JsonTypeName("fileSystemLoad")
@Data
@NoArgsConstructor
public class FileSystemLoadNode extends LoadNode implements InlongMetric, Serializable {

    private static final long serialVersionUID = -4836034838166667371L;

    private static final String trigger = "sink.partition-commit.trigger";
    private static final String delay = "sink.partition-commit.delay";
    private static final String policyKind = "sink.partition-commit.policy.kind";
    private static final String waterMarkZone = "sink.partition-commit.watermark-time-zone";
    private static final String rollingRolloverInterval = "sink.rolling-policy.rollover-interval";
    private static final String rollingPolicyFileSize = "sink.rolling-policy.file-size";

    @JsonProperty("format")
    @Nonnull
    private String format;

    @JsonProperty("path")
    @Nonnull
    private String path;

    @JsonProperty("partitionFields")
    private List<FieldInfo> partitionFields;

    @JsonProperty("tempTableName")
    private String tempTableName;

    @JsonProperty("serverTimeZone")
    private String serverTimeZone;

    @JsonCreator
    public FileSystemLoadNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @JsonProperty("fieldRelations") List<FieldRelation> fieldRelations,
            @JsonProperty("filters") List<FilterFunction> filters,
            @Nonnull @JsonProperty("path") String path,
            @Nonnull @JsonProperty("format") String format,
            @Nullable @JsonProperty("sinkParallelism") Integer sinkParallelism,
            @JsonProperty("properties") Map<String, String> properties,
            @JsonProperty("parFields") List<FieldInfo> partitionFields,
            @JsonProperty("serverTimeZone") String serverTimeZone) {
        super(id, name, fields, fieldRelations, filters, null, sinkParallelism, properties);
        this.format = Preconditions.checkNotNull(format, "format type is null");
        this.path = Preconditions.checkNotNull(path, "path is null");
        this.partitionFields = partitionFields;
        this.tempTableName = name;
        this.serverTimeZone = serverTimeZone;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> map = super.tableOptions();
        map.put("connector", "filesystem-inlong");
        map.put("path", path);
        map.put("format", format);
        if (null != partitionFields) {
            Map<String, String> properties = super.getProperties();
            if (null == properties || !properties.containsKey(trigger)) {
                map.put(trigger, "process-time");
            }
            if (null == properties || !properties.containsKey(delay)) {
                map.put(delay, "10s");
            }
            if (null == properties || !properties.containsKey(policyKind)) {
                map.put(policyKind, "metastore,success-file");
            }
        }
        if (!map.containsKey(rollingRolloverInterval)) {
            map.put(rollingRolloverInterval, "1min");
        }
        if (!map.containsKey(rollingPolicyFileSize)) {
            map.put(rollingPolicyFileSize, "128MB");
        }
        if (null != serverTimeZone && !map.containsKey(waterMarkZone)) {
            map.put(waterMarkZone, serverTimeZone);
        }
        return map;
    }

    @Override
    public String genTableName() {
        return "node_" + super.getId() + "_" + tempTableName;
    }

}
