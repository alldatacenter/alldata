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

package org.apache.inlong.sort.cdc.base.source.meta.split;

import com.ververica.cdc.connectors.base.utils.SerializerUtils;
import io.debezium.document.Document;
import io.debezium.document.DocumentReader;
import io.debezium.document.DocumentWriter;
import io.debezium.relational.TableId;
import io.debezium.relational.history.TableChanges.TableChange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.utils.LogicalTypeParser;
import org.apache.inlong.sort.cdc.base.debezium.history.FlinkJsonTableChangeSerializer;
import org.apache.inlong.sort.cdc.base.source.meta.offset.Offset;
import org.apache.inlong.sort.cdc.base.source.meta.offset.OffsetDeserializerSerializer;
import org.apache.inlong.sort.cdc.base.source.meta.offset.OffsetFactory;
import org.apache.inlong.sort.cdc.base.source.meta.split.MetricSplit.TableMetric;

/** A serializer for the {@link SourceSplitBase}.
 * Copy from com.ververica:flink-cdc-base:2.3.0.
 * */
public abstract class SourceSplitSerializer
        implements
            SimpleVersionedSerializer<SourceSplitBase>,
            OffsetDeserializerSerializer {

    private static final int VERSION = 3;
    private static final ThreadLocal<DataOutputSerializer> SERIALIZER_CACHE =
            ThreadLocal.withInitial(() -> new DataOutputSerializer(64));

    private static final int SNAPSHOT_SPLIT_FLAG = 1;
    private static final int STREAM_SPLIT_FLAG = 2;
    private static final int METRIC_SPLIT_FLAG = 4;

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public byte[] serialize(SourceSplitBase split) throws IOException {
        if (split.isSnapshotSplit()) {
            final SnapshotSplit snapshotSplit = split.asSnapshotSplit();
            // optimization: the splits lazily cache their own serialized form
            if (snapshotSplit.serializedFormCache != null) {
                return snapshotSplit.serializedFormCache;
            }

            final DataOutputSerializer out = SERIALIZER_CACHE.get();
            out.writeInt(SNAPSHOT_SPLIT_FLAG);
            out.writeUTF(snapshotSplit.getTableId().toString());
            out.writeUTF(snapshotSplit.splitId());
            out.writeUTF(snapshotSplit.getSplitKeyType().asSerializableString());

            final Object[] splitStart = snapshotSplit.getSplitStart();
            final Object[] splitEnd = snapshotSplit.getSplitEnd();
            // rowToSerializedString deals null case
            out.writeUTF(SerializerUtils.rowToSerializedString(splitStart));
            out.writeUTF(SerializerUtils.rowToSerializedString(splitEnd));
            writeOffsetPosition(snapshotSplit.getHighWatermark(), out);
            writeTableSchemas(snapshotSplit.getTableSchemas(), out);
            final byte[] result = out.getCopyOfBuffer();
            out.clear();
            // optimization: cache the serialized from, so we avoid the byte work during repeated
            // serialization
            snapshotSplit.serializedFormCache = result;
            return result;
        } else if (split.isStreamSplit()) {
            final StreamSplit streamSplit = split.asStreamSplit();
            // optimization: the splits lazily cache their own serialized form
            if (streamSplit.serializedFormCache != null) {
                return streamSplit.serializedFormCache;
            }
            final DataOutputSerializer out = SERIALIZER_CACHE.get();
            out.writeInt(STREAM_SPLIT_FLAG);
            out.writeUTF(streamSplit.splitId());
            out.writeUTF("");
            writeOffsetPosition(streamSplit.getStartingOffset(), out);
            writeOffsetPosition(streamSplit.getEndingOffset(), out);
            writeFinishedSplitsInfo(streamSplit.getFinishedSnapshotSplitInfos(), out);
            writeTableSchemas(streamSplit.getTableSchemas(), out);
            out.writeInt(streamSplit.getTotalFinishedSplitSize());
            final byte[] result = out.getCopyOfBuffer();
            out.clear();
            // optimization: cache the serialized from, so we avoid the byte work during repeated
            // serialization
            streamSplit.serializedFormCache = result;
            return result;
        } else {
            final MetricSplit metricSplit = (MetricSplit) split;
            final DataOutputSerializer out = SERIALIZER_CACHE.get();
            out.writeInt(METRIC_SPLIT_FLAG);
            out.writeLong(metricSplit.getNumBytesIn());
            out.writeLong(metricSplit.getNumRecordsIn());
            writeReadPhaseMetric(metricSplit.getReadPhaseMetricMap(), out);
            writeTableMetrics(metricSplit.getTableMetricMap(), out);
            final byte[] result = out.getCopyOfBuffer();
            out.clear();
            return result;
        }
    }

    @Override
    public SourceSplitBase deserialize(int version, byte[] serialized) throws IOException {
        switch (version) {
            case 1:
            case 2:
            case 3:
                return deserializeSplit(version, serialized);
            default:
                throw new IOException("Unknown version: " + version);
        }
    }

    public SourceSplitBase deserializeSplit(int version, byte[] serialized) throws IOException {
        final DataInputDeserializer in = new DataInputDeserializer(serialized);

        int splitKind = in.readInt();
        if (splitKind == SNAPSHOT_SPLIT_FLAG) {
            TableId tableId = TableId.parse(in.readUTF());
            String splitId = in.readUTF();
            RowType splitKeyType = (RowType) LogicalTypeParser.parse(in.readUTF());
            Object[] splitBoundaryStart = SerializerUtils.serializedStringToRow(in.readUTF());
            Object[] splitBoundaryEnd = SerializerUtils.serializedStringToRow(in.readUTF());
            Offset highWatermark = readOffsetPosition(version, in);
            Map<TableId, TableChange> tableSchemas = readTableSchemas(version, in);

            return new SnapshotSplit(
                    tableId,
                    splitId,
                    splitKeyType,
                    splitBoundaryStart,
                    splitBoundaryEnd,
                    highWatermark,
                    tableSchemas);
        } else if (splitKind == STREAM_SPLIT_FLAG) {
            String splitId = in.readUTF();
            // skip split Key Type
            in.readUTF();
            Offset startingOffset = readOffsetPosition(version, in);
            Offset endingOffset = readOffsetPosition(version, in);
            List<FinishedSnapshotSplitInfo> finishedSplitsInfo =
                    readFinishedSplitsInfo(version, in);
            Map<TableId, TableChange> tableChangeMap = readTableSchemas(version, in);
            int totalFinishedSplitSize = finishedSplitsInfo.size();
            if (version == 3) {
                totalFinishedSplitSize = in.readInt();
            }
            in.releaseArrays();
            return new StreamSplit(
                    splitId,
                    startingOffset,
                    endingOffset,
                    finishedSplitsInfo,
                    tableChangeMap,
                    totalFinishedSplitSize);
        } else if (splitKind == METRIC_SPLIT_FLAG) {
            long numBytesIn = 0L;
            long numRecordsIn = 0L;
            if (in.available() > 0) {
                numBytesIn = in.readLong();
                numRecordsIn = in.readLong();
            }
            Map<String, Long> readPhaseMetricMap = readReadPhaseMetric(in);
            Map<String, TableMetric> tableMetricMap = readTableMetrics(in);
            return new MetricSplit(numBytesIn, numRecordsIn, readPhaseMetricMap, tableMetricMap);
        } else {
            throw new IOException("Unknown split kind: " + splitKind);
        }
    }

    public static void writeTableSchemas(
            Map<TableId, TableChange> tableSchemas, DataOutputSerializer out) throws IOException {
        FlinkJsonTableChangeSerializer jsonSerializer = new FlinkJsonTableChangeSerializer();
        DocumentWriter documentWriter = DocumentWriter.defaultWriter();
        final int size = tableSchemas.size();
        out.writeInt(size);
        for (Map.Entry<TableId, TableChange> entry : tableSchemas.entrySet()) {
            out.writeUTF(entry.getKey().toString());
            final String tableChangeStr =
                    documentWriter.write(jsonSerializer.toDocument(entry.getValue()));
            final byte[] tableChangeBytes = tableChangeStr.getBytes(StandardCharsets.UTF_8);
            out.writeInt(tableChangeBytes.length);
            out.write(tableChangeBytes);
        }
    }

    public static Map<TableId, TableChange> readTableSchemas(int version, DataInputDeserializer in)
            throws IOException {
        DocumentReader documentReader = DocumentReader.defaultReader();
        Map<TableId, TableChange> tableSchemas = new HashMap<>();
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            TableId tableId = TableId.parse(in.readUTF());
            final String tableChangeStr;
            switch (version) {
                case 1:
                    tableChangeStr = in.readUTF();
                    break;
                case 2:
                case 3:
                    final int len = in.readInt();
                    final byte[] bytes = new byte[len];
                    in.read(bytes);
                    tableChangeStr = new String(bytes, StandardCharsets.UTF_8);
                    break;
                default:
                    throw new IOException("Unknown version: " + version);
            }
            Document document = documentReader.read(tableChangeStr);
            TableChange tableChange = FlinkJsonTableChangeSerializer.fromDocument(document, true);
            tableSchemas.put(tableId, tableChange);
        }
        return tableSchemas;
    }

    private void writeFinishedSplitsInfo(
            List<FinishedSnapshotSplitInfo> finishedSplitsInfo, DataOutputSerializer out)
            throws IOException {
        final int size = finishedSplitsInfo.size();
        out.writeInt(size);
        for (FinishedSnapshotSplitInfo splitInfo : finishedSplitsInfo) {
            splitInfo.serialize(out);
        }
    }

    private List<FinishedSnapshotSplitInfo> readFinishedSplitsInfo(
            int version, DataInputDeserializer in) throws IOException {
        List<FinishedSnapshotSplitInfo> finishedSplitsInfo = new ArrayList<>();
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            TableId tableId = TableId.parse(in.readUTF());
            String splitId = in.readUTF();
            Object[] splitStart = SerializerUtils.serializedStringToRow(in.readUTF());
            Object[] splitEnd = SerializerUtils.serializedStringToRow(in.readUTF());
            OffsetFactory offsetFactory =
                    (OffsetFactory) SerializerUtils.serializedStringToObject(in.readUTF());
            Offset highWatermark = readOffsetPosition(version, in);

            finishedSplitsInfo.add(
                    new FinishedSnapshotSplitInfo(
                            tableId, splitId, splitStart, splitEnd, highWatermark, offsetFactory));
        }
        return finishedSplitsInfo;
    }

    private static void writeReadPhaseMetric(Map<String, Long> readPhaseMetrics, DataOutputSerializer out)
            throws IOException {
        final int size = readPhaseMetrics.size();
        out.writeInt(size);
        for (Map.Entry<String, Long> entry : readPhaseMetrics.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeLong(entry.getValue());
        }
    }

    private static Map<String, Long> readReadPhaseMetric(DataInputDeserializer in) throws IOException {
        Map<String, Long> readPhaseMetrics = new HashMap<>();
        if (in.available() > 0) {
            final int size = in.readInt();
            for (int i = 0; i < size; i++) {
                readPhaseMetrics.put(in.readUTF(), in.readLong());
            }
        }
        return readPhaseMetrics;
    }

    private static void writeTableMetrics(Map<String, TableMetric> tableMetrics, DataOutputSerializer out)
            throws IOException {
        final int size = tableMetrics.size();
        out.writeInt(size);
        for (Map.Entry<String, TableMetric> entry : tableMetrics.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeLong(entry.getValue().getNumRecordsIn());
            out.writeLong(entry.getValue().getNumBytesIn());
        }
    }

    private static Map<String, TableMetric> readTableMetrics(DataInputDeserializer in) throws IOException {
        Map<String, TableMetric> tableMetrics = new HashMap<>();
        if (in.available() > 0) {
            final int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String tableIdentify = in.readUTF();
                tableMetrics.put(tableIdentify, new TableMetric(in.readLong(), in.readLong()));
            }
        }
        return tableMetrics;
    }
}
