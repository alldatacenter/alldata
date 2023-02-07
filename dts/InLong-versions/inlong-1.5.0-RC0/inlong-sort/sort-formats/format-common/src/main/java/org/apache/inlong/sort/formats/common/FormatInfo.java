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

package org.apache.inlong.sort.formats.common;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/**
 * The format information for data types.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "string", value = StringFormatInfo.class),
        @JsonSubTypes.Type(name = "varchar", value = VarCharFormatInfo.class),
        @JsonSubTypes.Type(name = "boolean", value = BooleanFormatInfo.class),
        @JsonSubTypes.Type(name = "byte", value = ByteFormatInfo.class),
        @JsonSubTypes.Type(name = "short", value = ShortFormatInfo.class),
        @JsonSubTypes.Type(name = "int", value = IntFormatInfo.class),
        @JsonSubTypes.Type(name = "long", value = LongFormatInfo.class),
        @JsonSubTypes.Type(name = "float", value = FloatFormatInfo.class),
        @JsonSubTypes.Type(name = "double", value = DoubleFormatInfo.class),
        @JsonSubTypes.Type(name = "decimal", value = DecimalFormatInfo.class),
        @JsonSubTypes.Type(name = "time", value = TimeFormatInfo.class),
        @JsonSubTypes.Type(name = "date", value = DateFormatInfo.class),
        @JsonSubTypes.Type(name = "timestamp", value = TimestampFormatInfo.class),
        @JsonSubTypes.Type(name = "array", value = ArrayFormatInfo.class),
        @JsonSubTypes.Type(name = "map", value = MapFormatInfo.class),
        @JsonSubTypes.Type(name = "row", value = RowFormatInfo.class),
        @JsonSubTypes.Type(name = "binary", value = BinaryFormatInfo.class),
        @JsonSubTypes.Type(name = "null", value = NullFormatInfo.class),
        @JsonSubTypes.Type(name = "local_zoned_timestamp", value = LocalZonedTimestampFormatInfo.class),
        @JsonSubTypes.Type(name = "varbinary", value = VarBinaryFormatInfo.class)
})
public interface FormatInfo extends Serializable {

    /**
     * Returns the type represented by this format.
     *
     * @return the type represented by this format.
     */
    @JsonIgnore
    TypeInfo getTypeInfo();
}
