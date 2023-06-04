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

package org.apache.inlong.sort.formats.inlongmsg;

import com.google.common.base.Objects;
import org.apache.flink.api.common.functions.util.ListCollector;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Collector;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.sort.formats.base.collectors.TimestampedCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class InLongMsgDeserializationSchema implements DeserializationSchema<RowData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InLongMsgDeserializationSchema.class);

    /** Inner {@link DeserializationSchema} to deserialize {@link InLongMsg} inner packaged
     *  data buffer message */
    private final DeserializationSchema<RowData> deserializationSchema;

    /** {@link MetadataConverter} of how to produce metadata from {@link InLongMsg}. */
    private final MetadataConverter[] metadataConverters;

    /** {@link TypeInformation} of the produced {@link RowData} (physical + meta data). */
    private final TypeInformation<RowData> producedTypeInfo;

    /** status of error */
    private final boolean ignoreErrors;

    public InLongMsgDeserializationSchema(
            DeserializationSchema<RowData> schema,
            MetadataConverter[] metadataConverters,
            TypeInformation<RowData> producedTypeInfo,
            boolean ignoreErrors) {
        this.deserializationSchema = schema;
        this.metadataConverters = metadataConverters;
        this.producedTypeInfo = producedTypeInfo;
        this.ignoreErrors = ignoreErrors;
    }

    @Override
    public void open(InitializationContext context) throws Exception {
        deserializationSchema.open(context);
    }

    @Override
    public RowData deserialize(byte[] bytes) {
        throw new RuntimeException(
                "Please invoke DeserializationSchema#deserialize(byte[], Collector<RowData>) instead.");
    }

    @Override
    public void deserialize(byte[] message, Collector<RowData> out) throws IOException {
        InLongMsg inLongMsg = InLongMsg.parseFrom(message);

        for (String attr : inLongMsg.getAttrs()) {
            InLongMsgHead head;
            try {
                head = InLongMsgUtils.parseHead(attr);
            } catch (Throwable t) {
                if (ignoreErrors) {
                    LOGGER.warn("Ignore inlong msg attr({})parse error.", attr, t);
                    continue;
                }
                throw new IOException(
                        "Failed to deserialize InLongMsg row '" + new String(message) + "'.", t);
            }

            Iterator<byte[]> iterator = inLongMsg.getIterator(attr);
            if (iterator == null) {
                continue;
            }

            while (iterator.hasNext()) {

                byte[] bodyBytes = iterator.next();
                long bodyLength = bodyBytes == null ? 0 : bodyBytes.length;

                if (bodyLength == 0) {
                    continue;
                }

                if (out instanceof TimestampedCollector) {
                    ((TimestampedCollector<RowData>) out).resetTimestamp(head.getTime().getTime());
                }

                List<RowData> list = new ArrayList<>();
                ListCollector<RowData> collector = new ListCollector<>(list);
                deserializationSchema.deserialize(bodyBytes, collector);
                list.forEach(rowdata -> emitRow(head, (GenericRowData) rowdata, out));
            }
        }

    }

    @Override
    public boolean isEndOfStream(RowData rowData) {
        return false;
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return producedTypeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InLongMsgDeserializationSchema)) {
            return false;
        }
        InLongMsgDeserializationSchema that = (InLongMsgDeserializationSchema) o;
        return ignoreErrors == that.ignoreErrors
                && Objects.equal(Arrays.stream(metadataConverters).collect(Collectors.toList()),
                        Arrays.stream(that.metadataConverters).collect(Collectors.toList()))
                && Objects.equal(deserializationSchema, that.deserializationSchema)
                && Objects.equal(producedTypeInfo, that.producedTypeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deserializationSchema, metadataConverters, producedTypeInfo, ignoreErrors);
    }

    interface MetadataConverter extends Serializable {

        Object read(InLongMsgHead head);
    }

    /** add metadata column */
    private void emitRow(InLongMsgHead head, GenericRowData physicalRow, Collector<RowData> out) {

        if (metadataConverters.length == 0) {
            out.collect(physicalRow);
            return;
        }

        final int metadataArity = metadataConverters.length;
        final int physicalArity = physicalRow.getArity();

        final GenericRowData producedRow =
                new GenericRowData(physicalRow.getRowKind(), physicalArity + metadataArity);
        for (int physicalPos = 0; physicalPos < physicalArity; physicalPos++) {
            producedRow.setField(physicalPos, physicalRow.getField(physicalPos));
        }
        for (int metadataPos = 0; metadataPos < metadataArity; metadataPos++) {
            producedRow.setField(
                    physicalArity + metadataPos, metadataConverters[metadataPos].read(head));
        }
        out.collect(producedRow);
    }
}
