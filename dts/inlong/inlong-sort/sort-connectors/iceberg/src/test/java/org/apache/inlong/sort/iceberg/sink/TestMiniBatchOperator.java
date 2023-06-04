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

package org.apache.inlong.sort.iceberg.sink;

import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.RowData.FieldGetter;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.types.logical.BigIntType;
import org.apache.flink.table.types.logical.IntType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.RowType.RowField;
import org.apache.flink.table.types.logical.VarCharType;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.inlong.sort.iceberg.sink.collections.PartitionGroupBuffer;
import org.apache.inlong.sort.iceberg.sink.collections.PartitionGroupBuffer.BufferType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(Parameterized.class)
public class TestMiniBatchOperator {

    /**
     * - Pre aggregation、partitioned: Whether the final output data comes in the partition order.
     *                                 And the data has been aggregated, the same primary key is only the last
     * - Non pre aggregation、partitioned: Whether the final data data comes in the partition order,
     *                                 And the data is consistent with the number of entries
     * - UnPartitioned: Sent in order of entry
     * - Mem and Rocksdb: Different buffer type performance should be consistent
     */

    private final BufferType bufferType;
    private OneInputStreamOperatorTestHarness<RowData, RowData> testHarness;

    private final static RowData[] INPUT_DATA = new RowData[]{
            GenericRowData.of(1L, StringData.fromString("Jack"), 183, 160, StringData.fromString("2021-01-01")),
            GenericRowData.of(2L, StringData.fromString("Mary"), 178, 140, StringData.fromString("2022-01-01")),
            GenericRowData.of(4L, StringData.fromString("Tim"), 172, 135, StringData.fromString("2023-01-01")),
            GenericRowData.of(5L, StringData.fromString("Harry"), 155, 120, StringData.fromString("2021-01-01")),
            GenericRowData.of(5L, StringData.fromString("HarryLord"), 165, 130, StringData.fromString("2021-01-01")),
            GenericRowData.of(5L, StringData.fromString("HarryLord"), 174, 140, StringData.fromString("2021-02-01")),
            GenericRowData.of(5L, StringData.fromString("HarryLord"), 182, 140, StringData.fromString("2021-03-01"))
    };
    private final static RowType FLINK_SCHEMA =
            new RowType(
                    Arrays.asList(
                            new RowField("id", new BigIntType(false)),
                            new RowField("name", new VarCharType(VarCharType.MAX_LENGTH)),
                            new RowField("height", new IntType()),
                            new RowField("weight", new IntType()),
                            new RowField("dt", new VarCharType(VarCharType.MAX_LENGTH))));
    private final static Schema ICE_SCHEMA = FlinkSchemaUtil.convert(FlinkSchemaUtil.toSchema(FLINK_SCHEMA));
    private final static FieldGetter[] FIELD_GETTERS =
            IntStream.range(0, FLINK_SCHEMA.getFieldCount())
                    .mapToObj(i -> RowData.createFieldGetter(FLINK_SCHEMA.getTypeAt(i), i))
                    .toArray(RowData.FieldGetter[]::new);

    @Parameterized.Parameters(name = "bufferType = {0}")
    public static Object[][] parameters() {
        return new Object[][]{
                {BufferType.ROCKSDB},
                {BufferType.MEM}
        };
    }

    public TestMiniBatchOperator(BufferType bufferType) {
        this.bufferType = bufferType;
    }

    @After
    public void after() throws Exception {
        testHarness.close();
        testHarness = null;
    }

    @Test
    public void testPreAggPartition() throws Exception {
        setupOperator(preAggPartition());
        List<RowData> rowDataList = testHarness.extractOutputValues();

        Assert.assertEquals("Pre-aggregation data size must be unique (id, dt) size",
                rowDataList.size(), 6);
        Map<String, List<Integer>> map = IntStream.range(0, rowDataList.size())
                .boxed()
                .collect(
                        Collectors.groupingBy(
                                index -> rowDataList.get(index).getString(4).toString().substring(0, 4)));
        Assert.assertTrue("Data in the same partition must be continuous",
                map.values().stream().allMatch(this::isContinuously));
        testHarness.close();
    }

    @Test
    public void testNonPreAggPartition() throws Exception {
        setupOperator(nonPreAggPartition());
        List<RowData> rowDataList = testHarness.extractOutputValues();

        Assert.assertEquals("Non pre-aggregation data size must be same with input size",
                rowDataList.size(), 7);
        Map<String, List<Integer>> map = IntStream.range(0, rowDataList.size())
                .boxed()
                .collect(
                        Collectors.groupingBy(
                                index -> rowDataList.get(index).getString(4).toString().substring(0, 4)));
        Assert.assertTrue("Data in the same partition must be continuous",
                map.values().stream().allMatch(this::isContinuously));
    }

    @Test
    public void testUnPartition() throws Exception {
        setupOperator(nonPreAggUnPartition());
        List<RowData> rowDataList = testHarness.extractOutputValues();

        processBatchData(2L);

        Assert.assertEquals("Non pre-aggregation data size must be same with input size",
                rowDataList.size(), INPUT_DATA.length);
        Assert.assertTrue("input should be same with output",
                IntStream.range(0, rowDataList.size())
                        .allMatch(index -> Arrays.stream(FIELD_GETTERS)
                                .allMatch(getter -> {
                                    Object input = getter.getFieldOrNull(rowDataList.get(index));
                                    Object output = getter.getFieldOrNull(INPUT_DATA[index]);
                                    return Objects.equals(input, output);
                                })));
    }

    private void setupOperator(PartitionGroupBuffer buffer) throws Exception {
        IcebergMiniBatchGroupOperator operator = new IcebergMiniBatchGroupOperator(buffer);
        testHarness = new OneInputStreamOperatorTestHarness<>(operator, 1, 1, 0);
        testHarness.setup();
        testHarness.open();
        processBatchData(1L);
    }

    private void processBatchData(long checkpointId) throws Exception {
        for (RowData row : INPUT_DATA) {
            testHarness.processElement(row, 1L);
        }

        testHarness.prepareSnapshotPreBarrier(checkpointId);
    }

    private PartitionGroupBuffer preAggPartition() {
        Schema deleteSchema = ICE_SCHEMA.select("id", "dt");
        PartitionSpec partitionSpec = PartitionSpec.builderFor(ICE_SCHEMA)
                .withSpecId(1)
                .truncate("dt", 4)
                .build();
        PartitionKey partitionKey = new PartitionKey(partitionSpec, ICE_SCHEMA);
        PartitionGroupBuffer buffer = PartitionGroupBuffer.preAggInstance(
                FIELD_GETTERS,
                deleteSchema,
                ICE_SCHEMA,
                partitionKey,
                bufferType);
        return buffer;
    }

    private PartitionGroupBuffer nonPreAggPartition() {
        PartitionSpec partitionSpec = PartitionSpec.builderFor(ICE_SCHEMA)
                .withSpecId(1)
                .truncate("dt", 4)
                .build();
        PartitionKey partitionKey = new PartitionKey(partitionSpec, ICE_SCHEMA);
        PartitionGroupBuffer buffer = PartitionGroupBuffer.nonPreAggInstance(ICE_SCHEMA, partitionKey, bufferType);
        return buffer;
    }

    private PartitionGroupBuffer nonPreAggUnPartition() {
        PartitionSpec partitionSpec = PartitionSpec.builderFor(ICE_SCHEMA)
                .withSpecId(1)
                .build();
        PartitionKey partitionKey = new PartitionKey(partitionSpec, ICE_SCHEMA);
        PartitionGroupBuffer buffer = PartitionGroupBuffer.nonPreAggInstance(ICE_SCHEMA, partitionKey, bufferType);
        return buffer;
    }

    private boolean isContinuously(List<Integer> index) {
        if (index == null || index.size() < 2) {
            return true;
        }

        for (int i = 1; i < index.size(); i++) {
            if (index.get(i) - index.get(i - 1) != 1) {
                return false;
            }
        }
        return true;
    }
}
