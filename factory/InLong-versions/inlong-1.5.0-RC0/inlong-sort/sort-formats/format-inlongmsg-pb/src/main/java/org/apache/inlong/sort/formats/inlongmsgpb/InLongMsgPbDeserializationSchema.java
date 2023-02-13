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

package org.apache.inlong.sort.formats.inlongmsgpb;

import com.google.common.base.Objects;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Collector;
import org.apache.inlong.common.msg.InLongMsg;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObj;
import org.apache.inlong.sdk.commons.protocol.ProxySdk.MessageObjs;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * InLongMsg pb format deserialization schema.
 * Used to deserialize {@link MessageObj} msg.
 */
public class InLongMsgPbDeserializationSchema implements DeserializationSchema<RowData> {

    /** Inner {@link DeserializationSchema} to deserialize {@link InLongMsg} inner packaged
     *  data buffer message */
    private final DeserializationSchema<RowData> deserializationSchema;

    /** {@link MetadataConverter} of how to produce metadata from {@link InLongMsg}. */
    private final MetadataConverter[] metadataConverters;

    /** {@link TypeInformation} of the produced {@link RowData} (physical + meta data). */
    private final TypeInformation<RowData> producedTypeInfo;

    /** status of error */
    private final boolean ignoreErrors;

    /** decompressor */
    private final InLongPbMsgDecompressor decompressor;

    public InLongMsgPbDeserializationSchema(
            DeserializationSchema<RowData> schema,
            MetadataConverter[] metadataConverters,
            TypeInformation<RowData> producedTypeInfo,
            InLongPbMsgDecompressor decompressor,
            boolean ignoreErrors) {
        this.deserializationSchema = schema;
        this.metadataConverters = metadataConverters;
        this.producedTypeInfo = producedTypeInfo;
        this.decompressor = decompressor;
        this.ignoreErrors = ignoreErrors;
    }

    @Override
    public RowData deserialize(byte[] bytes) throws IOException {
        throw new RuntimeException("Unsupported method, "
                + "Please invoke DeserializationSchema#deserialize(byte[], Collector<RowData>) instead.");
    }

    @Override
    public void deserialize(byte[] message, Collector<RowData> out) throws IOException {
        byte[] decompressed = decompressor.decompress(message);
        MessageObjs msgObjs = MessageObjs.parseFrom(decompressed);
        List<MessageObj> msgList = msgObjs.getMsgsList();
        for (MessageObj msg : msgList) {
            RowData row = deserializationSchema.deserialize(msg.getBody().toByteArray());
            this.emitRow(msg, (GenericRowData) row, out);
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
        if (!(o instanceof InLongMsgPbDeserializationSchema)) {
            return false;
        }
        InLongMsgPbDeserializationSchema that = (InLongMsgPbDeserializationSchema) o;
        return ignoreErrors == that.ignoreErrors
                && Objects.equal(Arrays.stream(metadataConverters).collect(Collectors.toList()),
                        Arrays.stream(that.metadataConverters).collect(Collectors.toList()))
                && Objects.equal(deserializationSchema, that.deserializationSchema)
                && Objects.equal(decompressor, that.decompressor)
                && Objects.equal(producedTypeInfo, that.producedTypeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deserializationSchema, metadataConverters, producedTypeInfo,
                ignoreErrors, decompressor);
    }

    interface MetadataConverter extends Serializable {

        Object read(MessageObj body);
    }

    interface InLongPbMsgDecompressor extends Serializable {

        byte[] decompress(byte[] message) throws IOException;
    }

    /** add metadata column */
    private void emitRow(MessageObj message, GenericRowData physicalRow, Collector<RowData> out) {
        if (metadataConverters.length == 0) {
            out.collect(physicalRow);
            return;
        }
        final int physicalArity = physicalRow.getArity();
        final int metadataArity = metadataConverters.length;
        final GenericRowData producedRow =
                new GenericRowData(physicalRow.getRowKind(), physicalArity + metadataArity);
        for (int physicalPos = 0; physicalPos < physicalArity; physicalPos++) {
            producedRow.setField(physicalPos, physicalRow.getField(physicalPos));
        }
        for (int metadataPos = 0; metadataPos < metadataArity; metadataPos++) {
            producedRow.setField(
                    physicalArity + metadataPos, metadataConverters[metadataPos].read(message));
        }
        out.collect(producedRow);
    }
}
