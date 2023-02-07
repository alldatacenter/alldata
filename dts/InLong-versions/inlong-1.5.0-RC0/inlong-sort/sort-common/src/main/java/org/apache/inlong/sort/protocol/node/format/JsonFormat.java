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
import lombok.ToString;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.HashMap;
import java.util.Map;

/**
 * The Json format
 *
 * @see <a herf="https://nightlies.apache.org/flink/flink-docs-release-1.13/zh/docs/connectors/table/formats/json/">
 *         Json Format</a>
 */

@JsonTypeName("jsonFormat")
@Data
@ToString
public class JsonFormat implements Format {

    private static final long serialVersionUID = 1L;

    private static final String IDENTIFIER = "json";

    @JsonProperty(value = "failOnMissingField", defaultValue = "false")
    private Boolean failOnMissingField;
    @JsonProperty(value = "ignoreParseErrors", defaultValue = "true")
    private Boolean ignoreParseErrors;
    @JsonProperty(value = "timestampFormatStandard", defaultValue = "SQL")
    private String timestampFormatStandard;
    @JsonProperty(value = "mapNullKeyMode", defaultValue = "DROP")
    private String mapNullKeyMode;
    @JsonProperty(value = "mapNullKeyLiteral", defaultValue = "null")
    private String mapNullKeyLiteral;
    @JsonProperty(value = "encodeDecimalAsPlainNumber", defaultValue = "true")
    private Boolean encodeDecimalAsPlainNumber;

    @JsonCreator
    public JsonFormat(@JsonProperty(value = "failOnMissingField", defaultValue = "false") Boolean failOnMissingField,
            @JsonProperty(value = "ignoreParseErrors", defaultValue = "true") Boolean ignoreParseErrors,
            @JsonProperty(value = "timestampFormatStandard", defaultValue = "SQL") String timestampFormatStandard,
            @JsonProperty(value = "mapNullKeyMode", defaultValue = "DROP") String mapNullKeyMode,
            @JsonProperty(value = "mapNullKeyLiteral", defaultValue = "null") String mapNullKeyLiteral,
            @JsonProperty(value = "encodeDecimalAsPlainNumber", defaultValue = "true") Boolean encodeDecimalAsPlainNumber) {
        this.failOnMissingField = failOnMissingField;
        this.ignoreParseErrors = ignoreParseErrors;
        this.timestampFormatStandard = timestampFormatStandard;
        this.mapNullKeyMode = mapNullKeyMode;
        this.mapNullKeyLiteral = mapNullKeyLiteral;
        this.encodeDecimalAsPlainNumber = encodeDecimalAsPlainNumber;
    }

    @JsonCreator
    public JsonFormat() {
        this(false, true, "SQL", "DROP", "null", true);
    }

    /**
     * Return json
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
     * Generate options for connector
     *
     * @return options
     */
    @Override
    public Map<String, String> generateOptions() {
        Map<String, String> options = new HashMap<>(16);
        options.put("format", getFormat());
        if (this.failOnMissingField != null) {
            String failOnMissingField = this.failOnMissingField.toString();
            options.put("json.fail-on-missing-field", failOnMissingField);
        }
        if (this.ignoreParseErrors != null) {
            String ignoreParseErrors = this.ignoreParseErrors.toString();
            options.put("json.ignore-parse-errors", ignoreParseErrors);
        }
        options.put("json.timestamp-format.standard", this.timestampFormatStandard);
        options.put("json.map-null-key.mode", this.mapNullKeyMode);
        options.put("json.map-null-key.literal", this.mapNullKeyLiteral);
        if (this.encodeDecimalAsPlainNumber != null) {
            String encodeDecimalAsPlainNumber = this.encodeDecimalAsPlainNumber.toString();
            options.put("json.encode.decimal-as-plain-number", encodeDecimalAsPlainNumber);
        }
        return options;
    }
}
