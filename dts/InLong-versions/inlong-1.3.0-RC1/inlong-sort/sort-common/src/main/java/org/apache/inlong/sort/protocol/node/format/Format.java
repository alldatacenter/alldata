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

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * Interface class for data format
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JsonFormat.class, name = "jsonFormat"),
        @JsonSubTypes.Type(value = AvroFormat.class, name = "avroFormat"),
        @JsonSubTypes.Type(value = DebeziumJsonFormat.class, name = "debeziumJsonFormat"),
        @JsonSubTypes.Type(value = CanalJsonFormat.class, name = "canalJsonFormat"),
        @JsonSubTypes.Type(value = CsvFormat.class, name = "csvFormat"),
        @JsonSubTypes.Type(value = InLongMsgFormat.class, name = "inLongMsgFormat"),
        @JsonSubTypes.Type(value = RawFormat.class, name = "rawFormat")
})
public interface Format extends Serializable {

    /**
     * return format for example json/avro/debezium-json/canal-json
     *
     * @return format
     */
    String getFormat();

    /**
     * generate options for connector
     *
     * @param includePrefix true will need append key and value when format is json avro csv
     * @return options
     */
    default Map<String, String> generateOptions(boolean includePrefix) {
        Map<String, String> options = generateOptions();
        if (includePrefix) {
            String key = "key.";
            String value = "value.";
            Map<String, String> includePrefixOptions = new HashMap<>(32);
            for (Map.Entry<String, String> option : options.entrySet()) {
                includePrefixOptions.put(key + option.getKey(), option.getValue());
                includePrefixOptions.put(value + option.getKey(), option.getValue());
            }
            options = includePrefixOptions;
        }
        return options;
    }

    Map<String, String> generateOptions();
}
