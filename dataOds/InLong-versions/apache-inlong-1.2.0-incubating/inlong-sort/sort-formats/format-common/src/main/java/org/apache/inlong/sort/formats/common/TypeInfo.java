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

import java.io.Serializable;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The type information for data types.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "string", value = StringTypeInfo.class),
        @JsonSubTypes.Type(name = "boolean", value = BooleanTypeInfo.class),
        @JsonSubTypes.Type(name = "byte", value = ByteTypeInfo.class),
        @JsonSubTypes.Type(name = "short", value = ShortTypeInfo.class),
        @JsonSubTypes.Type(name = "int", value = IntTypeInfo.class),
        @JsonSubTypes.Type(name = "long", value = LongTypeInfo.class),
        @JsonSubTypes.Type(name = "float", value = FloatTypeInfo.class),
        @JsonSubTypes.Type(name = "double", value = DoubleTypeInfo.class),
        @JsonSubTypes.Type(name = "decimal", value = DecimalTypeInfo.class),
        @JsonSubTypes.Type(name = "time", value = TimeTypeInfo.class),
        @JsonSubTypes.Type(name = "date", value = DateTypeInfo.class),
        @JsonSubTypes.Type(name = "timestamp", value = TimestampTypeInfo.class),
        @JsonSubTypes.Type(name = "array", value = ArrayTypeInfo.class),
        @JsonSubTypes.Type(name = "map", value = MapTypeInfo.class),
        @JsonSubTypes.Type(name = "row", value = RowTypeInfo.class),
        @JsonSubTypes.Type(name = "binary", value = BinaryTypeInfo.class),
        @JsonSubTypes.Type(name = "null", value = NullTypeInfo.class),
        @JsonSubTypes.Type(name = "local_zoned_timestamp", value = LocalZonedTimestampTypeInfo.class)
})
public interface TypeInfo extends Serializable {

}
