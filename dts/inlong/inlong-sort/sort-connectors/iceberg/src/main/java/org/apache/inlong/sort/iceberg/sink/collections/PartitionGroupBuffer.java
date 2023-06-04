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

package org.apache.inlong.sort.iceberg.sink.collections;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.shaded.guava18.com.google.common.base.Preconditions;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.RowData.FieldGetter;
import org.apache.flink.table.data.util.RowDataUtil;
import org.apache.flink.table.runtime.operators.sort.SortUtil;
import org.apache.flink.table.runtime.typeutils.InternalTypeInfo;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.Schema;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.flink.RowDataWrapper;
import org.apache.iceberg.types.Types.NestedField;
import org.apache.inlong.sort.iceberg.sink.collections.KVBuffer.Convertor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A buffer that stores data grouped according to partition information, and caches data for
 * one checkpoint cycle each time.
 */
public abstract class PartitionGroupBuffer implements Serializable {

    private static final long serialVersionUID = 1L;
    protected final KVBuffer<?, RowData> buffer;
    protected final PartitionKey partitionKey; // partition key helper
    protected final Schema rowSchema; // the whole field schema
    protected final Set<String> allPartition;
    private transient RowDataWrapper wrapper;

    private PartitionGroupBuffer(
            KVBuffer<?, RowData> buffer,
            PartitionKey partitionKey,
            Schema rowSchema) {
        this.buffer = buffer;
        this.partitionKey = partitionKey;
        this.rowSchema = rowSchema;
        this.allPartition = new HashSet<>();
    }

    public abstract RowData add(RowData row);

    public void scanPartitions(Consumer<Tuple2<?, RowData>> consumer) throws IOException {
        DataOutputSerializer outputBuffer = new DataOutputSerializer(4096);
        for (String partition : allPartition) {
            StringSerializer.INSTANCE.serialize(partition, outputBuffer);
            try (Stream<? extends Tuple2<?, RowData>> stream = buffer.scan(outputBuffer.getCopyOfBuffer())) {
                stream.forEach(consumer);
            }
            outputBuffer.clear();
        }
    }

    public void close() throws IOException {
        if (buffer instanceof Closeable) {
            ((Closeable) buffer).close();
        }
    }

    public void clear() {
        buffer.clear();
        allPartition.clear();
    }

    protected RowDataWrapper lazyRowDataWrapper() {
        if (wrapper == null) {
            wrapper = new RowDataWrapper(FlinkSchemaUtil.convert(rowSchema), rowSchema.asStruct());
        }
        return wrapper;
    }

    /**
     * Initialize {@link PreAggPartitionGroupBuffer}
     *
     * @param fieldsGetter function to get object from {@link RowData}
     * @param deleteSchema equality fields schema
     * @param rowSchema row data schema
     * @param partitionKey partition key
     * @param type buffer type to store buffer data
     */
    public static PreAggPartitionGroupBuffer preAggInstance(
            FieldGetter[] fieldsGetter,
            Schema deleteSchema,
            Schema rowSchema,
            PartitionKey partitionKey,
            BufferType type) {
        // note: here because `NestedField` does not override equals function, so can not indexOf by `NestedField`
        int[] equalityFieldIndex = deleteSchema.columns().stream()
                .map(field -> rowSchema.columns()
                        .stream()
                        .map(NestedField::fieldId)
                        .collect(Collectors.toList())
                        .indexOf(field.fieldId()))
                .sorted()
                .mapToInt(Integer::valueOf)
                .toArray();
        // do some check, check whether index is legal. can not be null and unique, and number in fields range.
        Preconditions.checkArgument(
                Arrays.stream(equalityFieldIndex)
                        .allMatch(index -> index >= 0 && index < fieldsGetter.length),
                String.format("Any equality field index (%s) should in a legal range.",
                        Arrays.toString(equalityFieldIndex)));

        KVBuffer<Tuple2<String, RowData>, RowData> buffer = null;
        if (BufferType.ROCKSDB.equals(type)) {
            TypeInformation<Tuple2<String, RowData>> keyTypeInfo = new TupleTypeInfo(
                    BasicTypeInfo.STRING_TYPE_INFO,
                    InternalTypeInfo.of(FlinkSchemaUtil.convert(deleteSchema)));
            TypeInformation<RowData> valueTypeInfo = InternalTypeInfo.of(FlinkSchemaUtil.convert(rowSchema));
            buffer = new RocksDBKVBuffer<>(
                    keyTypeInfo.createSerializer(new ExecutionConfig()),
                    valueTypeInfo.createSerializer(new ExecutionConfig()),
                    System.getProperty("java.io.tmpdir") + File.separator + "mini_batch");
        } else {
            TypeInformation<RowData> keyTypeInfo = InternalTypeInfo.of(FlinkSchemaUtil.convert(deleteSchema));
            buffer = new SortedHeapKVBuffer<>(
                    new PreAggPartitionConvertor(keyTypeInfo.createSerializer(new ExecutionConfig())));
        }
        return new PreAggPartitionGroupBuffer(buffer, partitionKey, rowSchema, fieldsGetter, equalityFieldIndex);
    }

    /**
     * Initialize {@link NonPreAggPartitionGroupBuffer}
     *
     * @param rowSchema row data schema
     * @param partitionKey partition key
     */
    public static NonPreAggPartitionGroupBuffer nonPreAggInstance(
            Schema rowSchema,
            PartitionKey partitionKey,
            BufferType type) {
        TypeInformation<Tuple2<String, Long>> keyTypeInfo = new TupleTypeInfo(
                BasicTypeInfo.STRING_TYPE_INFO,
                BasicTypeInfo.LONG_TYPE_INFO);
        TypeInformation<RowData> valueTypeInfo = InternalTypeInfo.of(FlinkSchemaUtil.convert(rowSchema));

        KVBuffer<Tuple2<String, Long>, RowData> buffer = null;
        if (BufferType.ROCKSDB.equals(type)) {
            buffer = new RocksDBKVBuffer<>(
                    keyTypeInfo.createSerializer(new ExecutionConfig()),
                    valueTypeInfo.createSerializer(new ExecutionConfig()),
                    System.getProperty("java.io.tmpdir") + "/mini_batch");
        } else {
            buffer = new SortedHeapKVBuffer<>(new NonPreAggPartitionConvetor());
        }
        return new NonPreAggPartitionGroupBuffer(buffer, partitionKey, rowSchema);
    }

    // The key is Tuple<partition path, primary key value>
    public static class PreAggPartitionGroupBuffer extends PartitionGroupBuffer {

        private final int[] equalityFieldIndex; // the position ordered of equality field in row schema
        private final FieldGetter[] fieldsGetter;

        private PreAggPartitionGroupBuffer(
                KVBuffer<Tuple2<String, RowData>, RowData> buffer,
                PartitionKey partitionKey,
                Schema rowSchema,
                FieldGetter[] fieldsGetter,
                int[] equalityFieldIndex) {
            super(buffer, partitionKey, rowSchema);
            this.fieldsGetter = fieldsGetter;
            this.equalityFieldIndex = equalityFieldIndex;

        }

        @Override
        public RowData add(RowData row) {
            RowData primaryKey = GenericRowData.of(Arrays.stream(equalityFieldIndex)
                    .boxed()
                    .map(index -> fieldsGetter[index].getFieldOrNull(row))
                    .toArray(Object[]::new));
            partitionKey.partition(lazyRowDataWrapper().wrap(row));
            allPartition.add(partitionKey.toPath());

            if (RowDataUtil.isAccumulateMsg(row)) {
                return ((KVBuffer<Tuple2<String, RowData>, RowData>) buffer).put(
                        new Tuple2<>(partitionKey.toPath(), primaryKey), row);
            } else {
                return ((KVBuffer<Tuple2<String, RowData>, RowData>) buffer).remove(
                        new Tuple2<>(partitionKey.toPath(), primaryKey));
            }
        }
    }

    // The key is Tuple<partition path, an incremental number>
    public static class NonPreAggPartitionGroupBuffer extends PartitionGroupBuffer {

        private final AtomicLong number;

        private NonPreAggPartitionGroupBuffer(
                KVBuffer<Tuple2<String, Long>, RowData> buffer,
                PartitionKey partitionKey,
                Schema rowSchema) {
            super(buffer, partitionKey, rowSchema);
            this.number = new AtomicLong(0L);
        }

        @Override
        public RowData add(RowData row) {
            partitionKey.partition(lazyRowDataWrapper().wrap(row));
            allPartition.add(partitionKey.toPath());

            return ((KVBuffer<Tuple2<String, Long>, RowData>) buffer).put(
                    new Tuple2<>(partitionKey.toPath(), number.incrementAndGet()), row);
        }

        @Override
        public void clear() {
            super.clear();
            number.set(0L);
        }
    }

    /**
     * A {@link Convertor} that key represent <partition, primary key>
     */
    public static class PreAggPartitionConvertor implements KVBuffer.Convertor<Tuple2<String, RowData>>, Serializable {

        private static final long serialVersionUID = 1L;
        private static final GenericRowData EMPTY_ROW = GenericRowData.of();

        private final TypeSerializer<RowData> keySerializer;
        private final DataOutputSerializer outputBuffer;
        private final DataInputDeserializer inputBuffer;

        public PreAggPartitionConvertor(TypeSerializer<RowData> keySerializer) {
            this.keySerializer = keySerializer;
            this.outputBuffer = new DataOutputSerializer(4096);
            this.inputBuffer = new DataInputDeserializer();
        }

        // note: Please do not put it into storage location, it's just for compare
        @Override
        public Tuple2<String, RowData> upper(byte[] keyPrefix) {
            inputBuffer.setBuffer(keyPrefix);
            try {
                return new Tuple2<>(StringSerializer.INSTANCE.deserialize(inputBuffer), GenericRowData.of());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // note: Please do not put it into storage location, it's just for compare
        @Override
        public Tuple2<String, RowData> lower(byte[] keyPrefix) {
            inputBuffer.setBuffer(keyPrefix);
            try {
                return new Tuple2<>(StringSerializer.INSTANCE.deserialize(inputBuffer), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int compare(Tuple2<String, RowData> k1, Tuple2<String, RowData> k2) {
            if (!Objects.equals(k1.f0, k2.f0)) {
                return Comparator.nullsLast(Comparator.comparing((Tuple2<String, RowData> k) -> k.f0)).compare(k1, k2);
            }

            if (k1.f1 == null || k2.f1 == null) {
                if (Objects.equals(k1.f1, k2.f1)) {
                    return 0;
                } else if (k1.f1 == null) {
                    return -1;
                } else {
                    return 1;
                }
            }

            if (EMPTY_ROW.equals(k1.f1) || EMPTY_ROW.equals(k2.f1)) {
                if (Objects.equals(k1.f1, k2.f1)) {
                    return 0;
                } else if (EMPTY_ROW.equals(k1.f1)) {
                    return 1;
                } else {
                    return -1;
                }
            }

            try {
                keySerializer.serialize(k1.f1, outputBuffer);
                byte[] b1 = outputBuffer.getCopyOfBuffer();
                outputBuffer.clear();
                keySerializer.serialize(k2.f1, outputBuffer);
                byte[] b2 = outputBuffer.getCopyOfBuffer();
                outputBuffer.clear();
                return SortUtil.compareBinary(b1, b2);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * A {@link Convertor} that key represent <partition, serial number>
     */
    public static class NonPreAggPartitionConvetor implements KVBuffer.Convertor<Tuple2<String, Long>>, Serializable {

        private static final long serialVersionUID = 1L;
        private final DataInputDeserializer inputBuffer = new DataInputDeserializer();

        @Override
        public Tuple2<String, Long> upper(byte[] keyPrefix) {
            inputBuffer.setBuffer(keyPrefix);
            try {
                return new Tuple2<>(StringSerializer.INSTANCE.deserialize(inputBuffer), Long.MAX_VALUE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Tuple2<String, Long> lower(byte[] keyPrefix) {
            inputBuffer.setBuffer(keyPrefix);
            try {
                return new Tuple2<>(StringSerializer.INSTANCE.deserialize(inputBuffer), Long.MIN_VALUE);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int compare(Tuple2<String, Long> k1, Tuple2<String, Long> k2) {
            if (!Objects.equals(k1.f0, k2.f0)) {
                return Comparator.nullsLast(Comparator.comparing((Tuple2<String, Long> k) -> k.f0)).compare(k1, k2);
            }
            return Comparator.comparing((Tuple2<String, Long> k) -> k.f1).compare(k1, k2);
        }
    }

    public enum BufferType {
        MEM,
        ROCKSDB
    }
}
