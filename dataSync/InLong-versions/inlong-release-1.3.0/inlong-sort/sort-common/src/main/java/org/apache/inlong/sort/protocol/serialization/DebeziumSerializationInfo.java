/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.protocol.serialization;

import java.util.Objects;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

public class DebeziumSerializationInfo implements SerializationInfo {

    private static final long serialVersionUID = 1L;

    @JsonProperty("timestamp_format_standard")
    private final String timestampFormatStandard;

    @JsonProperty("map_null_key_mod")
    private final String mapNullKeyMod;

    @JsonProperty("map_null_key_literal")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String mapNullKeyLiteral;

    @JsonProperty("encode_decimal_as_plain_number")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final boolean encodeDecimalAsPlainNumber;

    public DebeziumSerializationInfo(
            @JsonProperty("timestamp_format_standard") String timestampFormatStandard,
            @JsonProperty("map_null_key_mod") String mapNullKeyMod,
            @JsonProperty("map_null_key_literal") String mapNullKeyLiteral,
            @JsonProperty("encode_decimal_as_plain_number") boolean encodeDecimalAsPlainNumber) {
        this.timestampFormatStandard = timestampFormatStandard;
        this.mapNullKeyMod = mapNullKeyMod;
        this.mapNullKeyLiteral = mapNullKeyLiteral;
        this.encodeDecimalAsPlainNumber = encodeDecimalAsPlainNumber;
    }

    @JsonProperty("timestamp_format_standard")
    public String getTimestampFormatStandard() {
        return timestampFormatStandard;
    }

    @JsonProperty("map_null_key_mod")
    public String getMapNullKeyMod() {
        return mapNullKeyMod;
    }

    @JsonProperty("map_null_key_literal")
    public String getMapNullKeyLiteral() {
        return mapNullKeyLiteral;
    }

    @JsonProperty("encode_decimal_as_plain_number")
    public boolean isEncodeDecimalAsPlainNumber() {
        return encodeDecimalAsPlainNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DebeziumSerializationInfo that = (DebeziumSerializationInfo) o;
        return encodeDecimalAsPlainNumber == that.encodeDecimalAsPlainNumber
                && Objects.equals(timestampFormatStandard, that.timestampFormatStandard)
                && Objects.equals(mapNullKeyMod, that.mapNullKeyMod)
                && Objects.equals(mapNullKeyLiteral, that.mapNullKeyLiteral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestampFormatStandard, mapNullKeyMod, mapNullKeyLiteral, encodeDecimalAsPlainNumber);
    }

    @Override
    public String toString() {
        return "DebeziumSerializationInfo{"
                + "timestampFormatStandard='" + timestampFormatStandard + '\''
                + ", mapNullKeyMod='" + mapNullKeyMod + '\''
                + ", mapNullKeyLiteral='" + mapNullKeyLiteral + '\''
                + ", encodeDecimalAsPlainNumber=" + encodeDecimalAsPlainNumber
                + '}';
    }
}
