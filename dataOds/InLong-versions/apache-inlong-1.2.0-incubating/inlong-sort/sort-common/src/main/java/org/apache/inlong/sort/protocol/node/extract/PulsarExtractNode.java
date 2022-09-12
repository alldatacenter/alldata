/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.inlong.sort.protocol.node.extract;

import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@JsonTypeName("pulsarExtract")
@Data
public class PulsarExtractNode extends ExtractNode {
    private static final long serialVersionUID = 1L;

    @Nonnull
    @JsonProperty("topic")
    private String topic;
    @Nonnull
    @JsonProperty("adminUrl")
    private String adminUrl;
    @Nonnull
    @JsonProperty("serviceUrl")
    private String serviceUrl;
    @Nonnull
    @JsonProperty("format")
    private Format format;

    @JsonProperty("scanStartupMode")
    private String scanStartupMode;

    @JsonProperty("primaryKey")
    private String primaryKey;

    @JsonCreator
    public PulsarExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermarkField") WatermarkField watermarkField,
            @JsonProperty("properties") Map<String, String> properties,
            @Nonnull @JsonProperty("topic") String topic,
            @Nonnull @JsonProperty("adminUrl") String adminUrl,
            @Nonnull @JsonProperty("serviceUrl") String serviceUrl,
            @Nonnull @JsonProperty("format") Format format,
            @Nonnull @JsonProperty("scanStartupMode") String scanStartupMode,
            @JsonProperty("primaryKey") String primaryKey) {
        super(id, name, fields, watermarkField, properties);
        this.topic = Preconditions.checkNotNull(topic, "pulsar topic is null.");
        this.adminUrl = Preconditions.checkNotNull(adminUrl, "pulsar adminUrl is null.");
        this.serviceUrl = Preconditions.checkNotNull(serviceUrl, "pulsar serviceUrl is null.");
        this.format = Preconditions.checkNotNull(format, "pulsar format is null.");
        this.scanStartupMode = Preconditions.checkNotNull(scanStartupMode,
                "pulsar scanStartupMode is null.");
        this.primaryKey = primaryKey;
    }

    /**
     * generate table options
     *
     * @return options
     */
    @Override
    public Map<String, String> tableOptions() {
        Map<String, String> options = super.tableOptions();
        if (StringUtils.isEmpty(this.primaryKey)) {
            options.put("connector", "pulsar-inlong");
            options.putAll(format.generateOptions(false));
        } else {
            options.put("connector", "upsert-pulsar-inlong");
            options.putAll(format.generateOptions(true));
        }
        options.put("generic", "true");
        options.put("service-url", serviceUrl);
        options.put("admin-url", adminUrl);
        options.put("topic", topic);
        options.put("scan.startup.mode", scanStartupMode);

        return options;
    }

    @Override
    public String genTableName() {
        return String.format("table_%s", super.getId());
    }

    @Override
    public String getPrimaryKey() {
        return primaryKey;
    }

    @Override
    public List<FieldInfo> getPartitionFields() {
        return super.getPartitionFields();
    }
}
