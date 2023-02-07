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

package org.apache.inlong.sort.protocol.node.format;

import lombok.Data;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.HashMap;
import java.util.Map;

/**
 * The Avro format.
 *
 * @see <a href="https://nightlies.apache.org/flink/flink-docs-release-1.13/zh/docs/connectors/table/formats/avro/">Avro
 *         Format</a>
 */
@JsonTypeName("avroFormat")
@Data
public class AvroFormat implements Format {

    private static final long serialVersionUID = 1L;

    private static final String IDENTIFIER = "avro";

    @JsonProperty(value = "codec")
    private String codec;

    @JsonCreator
    public AvroFormat(@JsonProperty(value = "codec") String codec) {
        this.codec = codec;
    }

    @JsonCreator
    public AvroFormat() {
    }

    /**
     * Return avro
     *
     * @return format
     */
    @JsonIgnore
    @Override
    public String getFormat() {
        return IDENTIFIER;
    }

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    /**
     * generate options for connector
     *
     * @return options
     */
    @Override
    public Map<String, String> generateOptions() {
        Map<String, String> options = new HashMap<>(4);
        options.put("format", getFormat());
        return options;
    }
}
