
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

import java.io.Serializable;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface of deserialization info.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @Type(value = CsvDeserializationInfo.class, name = "csv"),
        @Type(value = AvroDeserializationInfo.class, name = "avro"),
        @Type(value = JsonDeserializationInfo.class, name = "json"),
        @Type(value = CanalDeserializationInfo.class, name = "canal"),
        @Type(value = DebeziumDeserializationInfo.class, name = "debezium_json"),
        @Type(value = InLongMsgCsvDeserializationInfo.class, name = "inlong_msg_csv"),
        @Type(value = InLongMsgCsv2DeserializationInfo.class, name = "inlong_msg_csv2"),
        @Type(value = InLongMsgKvDeserializationInfo.class, name = "inlong_msg_kv"),
        @Type(value = InLongMsgTlogCsvDeserializationInfo.class, name = "inlong_msg_tlog_csv"),
        @Type(value = InLongMsgTlogKvDeserializationInfo.class, name = "inlong_msg_tlog_kv")
})
public interface DeserializationInfo extends Serializable {

}
