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

package org.apache.inlong.sort.formats.common;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The format information for maps.
 */
public class MapFormatInfo implements FormatInfo {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_KEY_FORMAT = "keyFormat";
    private static final String FIELD_VALUE_FORMAT = "valueFormat";

    /**
     * The format information for keys in the map.
     */
    @JsonProperty(FIELD_KEY_FORMAT)
    @Nonnull
    private final FormatInfo keyFormatInfo;

    /**
     * The format information for values in the map.
     */
    @JsonProperty(FIELD_VALUE_FORMAT)
    @Nonnull
    private final FormatInfo valueFormatInfo;

    @JsonCreator
    public MapFormatInfo(
            @JsonProperty(FIELD_KEY_FORMAT) @Nonnull FormatInfo keyFormatInfo,
            @JsonProperty(FIELD_VALUE_FORMAT) @Nonnull FormatInfo valueFormatInfo
    ) {
        this.keyFormatInfo = keyFormatInfo;
        this.valueFormatInfo = valueFormatInfo;
    }

    @Nonnull
    public FormatInfo getKeyFormatInfo() {
        return keyFormatInfo;
    }

    @Nonnull
    public FormatInfo getValueFormatInfo() {
        return valueFormatInfo;
    }

    @Override
    public MapTypeInfo getTypeInfo() {
        TypeInfo keyTypeInfo = keyFormatInfo.getTypeInfo();
        TypeInfo valueTypeInfo = valueFormatInfo.getTypeInfo();

        return new MapTypeInfo(keyTypeInfo, valueTypeInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MapFormatInfo that = (MapFormatInfo) o;
        return keyFormatInfo.equals(that.keyFormatInfo)
                       && valueFormatInfo.equals(that.valueFormatInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyFormatInfo, valueFormatInfo);
    }

    @Override
    public String toString() {
        return "MapFormatInfo{" + "keyFormatInfo=" + keyFormatInfo + ", valueFormatInfo=" + valueFormatInfo + '}';
    }
}
