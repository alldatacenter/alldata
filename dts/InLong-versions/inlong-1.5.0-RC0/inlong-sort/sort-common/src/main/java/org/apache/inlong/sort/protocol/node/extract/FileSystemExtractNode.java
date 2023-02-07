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

package org.apache.inlong.sort.protocol.node.extract;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.ExtractNode;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The feature version of flink is greater than or equal to 1.16.x for stream table
 */
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("fileSystemExtract")
@Data
public class FileSystemExtractNode extends ExtractNode implements Serializable {

    private static final long serialVersionUID = 1944524675510533454L;

    private static final String sourceMonitorInterval = "source.monitor-interval";

    @JsonProperty("format")
    @Nonnull
    private String format;

    @JsonProperty("path")
    @Nonnull
    private String path;

    @JsonProperty("name")
    private String name;

    @JsonCreator
    public FileSystemExtractNode(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nonnull @JsonProperty("path") String path,
            @Nonnull @JsonProperty("format") String format,
            @JsonProperty("properties") Map<String, String> properties) {
        super(id, name, fields, null, properties);
        this.format = Preconditions.checkNotNull(format, "format type is null");
        this.path = Preconditions.checkNotNull(path, "path is null");
        this.name = name;
    }

    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> map = super.tableOptions();
        map.put("connector", "filesystem");
        map.put("path", path);
        map.put("format", format);
        if (null == getProperties() || !getProperties().containsKey(sourceMonitorInterval)) {
            map.put(sourceMonitorInterval, Duration.ofMinutes(1L).toString());
        }
        return map;
    }

    @Override
    public String genTableName() {
        return "node_" + super.getId() + "_" + name;
    }

}
