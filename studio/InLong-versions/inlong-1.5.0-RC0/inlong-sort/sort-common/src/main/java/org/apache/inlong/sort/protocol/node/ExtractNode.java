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
import org.apache.inlong.sort.protocol.transformation.WatermarkField;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * extract node extracts data from external system
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MySqlExtractNode.class, name = "mysqlExtract"),
        @JsonSubTypes.Type(value = KafkaExtractNode.class, name = "kafkaExtract"),
        @JsonSubTypes.Type(value = PostgresExtractNode.class, name = "postgresExtract"),
        @JsonSubTypes.Type(value = FileSystemExtractNode.class, name = "fileSystemExtract"),
        @JsonSubTypes.Type(value = MongoExtractNode.class, name = "mongoExtract"),
        @JsonSubTypes.Type(value = SqlServerExtractNode.class, name = "sqlserverExtract"),
        @JsonSubTypes.Type(value = OracleExtractNode.class, name = "oracleExtract"),
        @JsonSubTypes.Type(value = TubeMQExtractNode.class, name = "tubeMQExtract"),
        @JsonSubTypes.Type(value = PulsarExtractNode.class, name = "pulsarExtract"),
        @JsonSubTypes.Type(value = RedisExtractNode.class, name = "redisExtract"),
        @JsonSubTypes.Type(value = DorisExtractNode.class, name = "dorisExtract"),
        @JsonSubTypes.Type(value = HudiExtractNode.class, name = "hudiExtract"),
})
@Data
@NoArgsConstructor
public abstract class ExtractNode implements Node {

    @JsonProperty("id")
    private String id;
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("name")
    private String name;
    @JsonProperty("fields")
    private List<FieldInfo> fields;
    @Nullable
    @JsonProperty("watermarkField")
    @JsonInclude(Include.NON_NULL)
    private WatermarkField watermarkField;
    @Nullable
    @JsonInclude(Include.NON_NULL)
    @JsonProperty("properties")
    private Map<String, String> properties;

    @JsonCreator
    public ExtractNode(@JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("fields") List<FieldInfo> fields,
            @Nullable @JsonProperty("watermark_field") WatermarkField watermarkField,
            @Nullable @JsonProperty("properties") Map<String, String> properties) {
        this.id = Preconditions.checkNotNull(id, "id is null");
        this.name = name;
        this.fields = Preconditions.checkNotNull(fields, "fields is null");
        Preconditions.checkState(!fields.isEmpty(), "fields is empty");
        this.watermarkField = watermarkField;
        this.properties = properties;
    }
}
