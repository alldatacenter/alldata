/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.plugins.incr.flink.cdc;

import com.qlangtech.tis.realtime.transfer.DTO;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;
import io.debezium.data.Envelope;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.util.Collector;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A JSON format implementation of {@link DebeziumDeserializationSchema} which deserializes the
 * received {@link SourceRecord} to JSON String.
 *
 * @see com.ververica.cdc.debezium.table.RowDataDebeziumDeserializeSchema
 */
public class TISDeserializationSchema implements DebeziumDeserializationSchema<DTO> {
    private static final Pattern PATTERN_TOPIC = Pattern.compile(".+\\.(.+)\\.(.+)");
    private static final long serialVersionUID = 1L;
    //private static final JsonConverter CONVERTER = new JsonConverter();
    private static final Logger logger = LoggerFactory.getLogger(TISDeserializationSchema.class);

    private final ISourceValConvert rawValConvert;
    private final Function<String, String> physicsTabName2LogicName;

    public TISDeserializationSchema(ISourceValConvert rawValConvert, Function<String, String> physicsTabName2LogicName) {
        this.rawValConvert = rawValConvert;
        this.physicsTabName2LogicName = physicsTabName2LogicName;
    }

    public TISDeserializationSchema() {
        this(new DefaultSourceValConvert(), new DefaultTableNameConvert());
    }

//    public TISDeserializationSchema(boolean includeSchema) {
//        final HashMap<String, Object> configs = new HashMap<>();
//        configs.put(ConverterConfig.TYPE_CONFIG, ConverterType.VALUE.getName());
//        configs.put(JsonConverterConfig.SCHEMAS_ENABLE_CONFIG, includeSchema);
//        CONVERTER.configure(configs);
//    }

    @Override
    public void deserialize(SourceRecord record, Collector<DTO> out) throws Exception {
        DTO dto = new DTO();
        Envelope.Operation op = Envelope.operationFor(record);
        Struct value = (Struct) record.value();
        Schema valueSchema = record.valueSchema();
        Matcher topicMatcher = PATTERN_TOPIC.matcher(record.topic());
        if (!topicMatcher.matches()) {
            throw new IllegalStateException("topic is illegal:" + record.topic());
        }
        dto.setDbName(topicMatcher.group(1));
        dto.setTableName(physicsTabName2LogicName.apply(topicMatcher.group(2)));

        if (op != Envelope.Operation.CREATE && op != Envelope.Operation.READ) {
            if (op == Envelope.Operation.DELETE) {
                this.extractBeforeRow(dto, value, valueSchema);
                dto.setEventType(DTO.EventType.DELETE);
                out.collect(dto);
            } else {
                this.extractBeforeRow(dto, value, valueSchema);
                this.extractAfterRow(dto, value, valueSchema);

//                StringBuffer log = new StringBuffer();
//                Object v = null;
//                for (Map.Entry<String, Object> e : dto.getAfter().entrySet()) {
//                    v = e.getValue();
//                    if (v == null) {
//                        continue;
//                    }
//                    log.append(e.getKey()).append(":").append(v.getClass().getName()).append("||");
//                }
//                logger.info(log.toString());
                //  }
                // TODO: 需要判断这条记录是否要处理
                dto.setEventType(DTO.EventType.UPDATE_BEFORE);
                out.collect(dto);

                dto = dto.colone();
                dto.setEventType(DTO.EventType.UPDATE_AFTER);
                out.collect(dto);
            }
        } else {
            this.extractAfterRow(dto, value, valueSchema);
//            this.validator.validate(delete, RowKind.INSERT);
            dto.setEventType(DTO.EventType.ADD);
            out.collect(dto);
        }
    }


    private void extractAfterRow(DTO dto, Struct value, Schema valueSchema) {
        Schema afterSchema = valueSchema.field("after").schema();
        Struct after = value.getStruct("after");

        Map<String, Object> afterVals = new HashMap<>();
        for (Field f : afterSchema.fields()) {
            afterVals.put(f.name(), rawValConvert.convert(dto, f, after.get(f.name())));
        }
        dto.setAfter(afterVals);
    }


    private void extractBeforeRow(DTO dto, Struct value, Schema valueSchema) {
        Schema beforeSchema = valueSchema.field("before").schema();
        Struct before = value.getStruct("before");
        Map<String, Object> beforeVals = new HashMap<>();
        for (Field f : beforeSchema.fields()) {
            beforeVals.put(f.name(), rawValConvert.convert(dto, f, before.get(f.name())));
        }
        dto.setBefore(beforeVals);
    }

    @Override
    public TypeInformation<DTO> getProducedType() {
        return TypeInformation.of(DTO.class);
    }
}
