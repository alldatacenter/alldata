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

package org.apache.inlong.sort.protocol.deserialization;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Canal deserialization info
 */
public class CanalDeserializationInfo implements DeserializationInfo {

    private static final long serialVersionUID = -5344203248610337314L;

    @JsonProperty("database")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String database;

    @JsonProperty("table")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String table;

    @JsonProperty("ignore_parse_errors")
    private final boolean ignoreParseErrors;

    @JsonProperty("timestamp_format_standard")
    private final String timestampFormatStandard;

    @Deprecated
    @JsonProperty("include_metadata")
    private final boolean includeMetadata;

    @JsonCreator
    public CanalDeserializationInfo(
            @JsonProperty("database") String database,
            @JsonProperty("table") String table,
            @JsonProperty("ignore_parse_errors") boolean ignoreParseErrors,
            @JsonProperty("timestamp_format_standard") String timestampFormatStandard,
            @JsonProperty("include_metadata") boolean includeMetadata) {
        this.database = database;
        this.table = table;
        this.ignoreParseErrors = ignoreParseErrors;
        this.timestampFormatStandard = timestampFormatStandard;
        this.includeMetadata = includeMetadata;
    }

    @JsonProperty("database")
    public String getDatabase() {
        return database;
    }

    @JsonProperty("table")
    public String getTable() {
        return table;
    }

    @JsonProperty("ignore_parse_errors")
    public boolean isIgnoreParseErrors() {
        return ignoreParseErrors;
    }

    @JsonProperty("timestamp_format_standard")
    public String getTimestampFormatStandard() {
        return timestampFormatStandard;
    }

    @JsonProperty("include_metadata")
    public boolean isIncludeMetadata() {
        return includeMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CanalDeserializationInfo that = (CanalDeserializationInfo) o;
        return ignoreParseErrors == that.ignoreParseErrors
                && includeMetadata == that.includeMetadata
                && Objects.equals(database, that.database)
                && Objects.equals(table, that.table)
                && Objects.equals(timestampFormatStandard, that.timestampFormatStandard);
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, table, ignoreParseErrors, timestampFormatStandard, includeMetadata);
    }

}
