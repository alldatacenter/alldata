/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.parquet.columnreaders;

import java.math.BigDecimal;

import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.BatchSizingMemoryUtil;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.BatchSizingMemoryUtil.ColumnMemoryUsageInfo;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.ColumnMemoryQuota;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(UnlikelyTest.class)
public class TestBatchSizingMemoryUtil extends PhysicalOpUnitTestBase {

  // Batch schema
  private static TupleMetadata schema;
  private static TupleMetadata nullableSchema;

  // Row set
  private RowSet.SingleRowSet rowSet;

  // Column memory usage information
  private final ColumnMemoryUsageInfo[] columnMemoryInfo = new ColumnMemoryUsageInfo[3];

  @BeforeClass
  public static void setUpBeforeClass() {
    schema = new SchemaBuilder()
      .add("name_vchar", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.REQUIRED)
      .add("name_vbinary", TypeProtos.MinorType.VARBINARY, TypeProtos.DataMode.REQUIRED)
      .add("name_vdecimal", TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.REQUIRED)
      .buildSchema();

    nullableSchema = new SchemaBuilder()
      .add("name_vchar", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("name_vbinary", TypeProtos.MinorType.VARBINARY, TypeProtos.DataMode.OPTIONAL)
      .add("name_vdecimal", TypeProtos.MinorType.VARDECIMAL, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();
  }

  @Test
  public void testCanAddNewData() {
   try {
     testCanAddNewData(false);

    } finally {
      if (rowSet != null) {
        rowSet.clear();
      }
    }
  }

  @Test
  public void testCanAddNewNullabalData() {
   try {
     testCanAddNewData(true);

    } finally {
      if (rowSet != null) {
        rowSet.clear();
      }
    }
  }

  private void testCanAddNewData(boolean isOptional) {
    final Object[] data = {"0123456789", new byte[10], new BigDecimal(Long.MAX_VALUE) };
    final int numRows = 1;

    // Load the test data into the associated Value Vectors
    loadTestData(numRows, isOptional, data);

    for (int columnIdx = 0; columnIdx < 3; columnIdx++) {
      final ColumnMemoryUsageInfo columnInfo = columnMemoryInfo[columnIdx];
      final long remainingBitsCapacity = getRemainingBitsCapacity(columnInfo);
      final long remainingOffsetsCapacity = getRemainingOffsetsCapacity(columnInfo);
      final long remainingDataCapacity = getRemainingDataCapacity(columnInfo);

      // Test current VV is within quota (since we are not adding new entries)
      Assert.assertTrue(BatchSizingMemoryUtil.canAddNewData(columnInfo, 0, 0, 0));

      if (isOptional) {
        // Test add the maximum offsets and data
        Assert.assertTrue(BatchSizingMemoryUtil.canAddNewData(columnInfo, remainingBitsCapacity, remainingOffsetsCapacity, remainingDataCapacity));

        // Test VV overflow: for bits, offsets, data, and then all
        Assert.assertFalse(BatchSizingMemoryUtil.canAddNewData(columnInfo, remainingBitsCapacity + 1, remainingOffsetsCapacity, remainingDataCapacity));
        Assert.assertFalse(BatchSizingMemoryUtil.canAddNewData(columnInfo, remainingBitsCapacity, remainingOffsetsCapacity + 1, remainingDataCapacity));
        Assert.assertFalse(BatchSizingMemoryUtil.canAddNewData(columnInfo, remainingBitsCapacity, remainingOffsetsCapacity, remainingDataCapacity + 1));
        Assert.assertFalse(BatchSizingMemoryUtil.canAddNewData(columnInfo, remainingBitsCapacity + 1, remainingOffsetsCapacity + 1, remainingDataCapacity + 1));
      } else {
        // Test add the maximum offsets and data
        Assert.assertTrue(BatchSizingMemoryUtil.canAddNewData(columnInfo, 0, remainingOffsetsCapacity, remainingDataCapacity));

        // Test VV overflow: for offsets, data, and then both
        Assert.assertFalse(BatchSizingMemoryUtil.canAddNewData(columnInfo, 0, remainingOffsetsCapacity + 1, remainingDataCapacity));
        Assert.assertFalse(BatchSizingMemoryUtil.canAddNewData(columnInfo, 0, remainingOffsetsCapacity, remainingDataCapacity + 1));
        Assert.assertFalse(BatchSizingMemoryUtil.canAddNewData(columnInfo, 0, remainingOffsetsCapacity + 1, remainingDataCapacity + 1));
      }
    }
  }

  private void loadTestData(int numRows, boolean isOptional, Object...data) {
    // First, lets create a row set
    rowSet = null;
    final TupleMetadata targetSchema = isOptional ? nullableSchema : schema;
    final RowSetBuilder builder = operatorFixture.rowSetBuilder(targetSchema);

    for (int rowIdx = 0; rowIdx < numRows; ++rowIdx) {
      builder.addRow(data);
    }
    rowSet = builder.build();

    // Now load the column memory information
    for (int columnIdx = 0; columnIdx < columnMemoryInfo.length; columnIdx++) {
      columnMemoryInfo[columnIdx] = getColumnMemoryUsageInfo(columnIdx);
    }
  }

  private ColumnMemoryUsageInfo getColumnMemoryUsageInfo(int columnIdx) {
    final VectorContainer vectorContainer = rowSet.container();
    final ColumnMemoryUsageInfo result = new ColumnMemoryUsageInfo();

    result.vector = vectorContainer.getValueVector(columnIdx).getValueVector();
    result.currValueCount = vectorContainer.getRecordCount();
    result.memoryQuota = new ColumnMemoryQuota(result.vector.getAllocatedSize());

    // Load the VV memory usage information
    BatchSizingMemoryUtil.getMemoryUsage(result.vector, result.currValueCount, result.vectorMemoryUsage);

    return result;
  }

  private static long getRemainingBitsCapacity(ColumnMemoryUsageInfo columnInfo) {
    return columnInfo.vectorMemoryUsage.bitsBytesCapacity - columnInfo.vectorMemoryUsage.bitsBytesUsed;
  }

  private static long getRemainingOffsetsCapacity(ColumnMemoryUsageInfo columnInfo) {
    return columnInfo.vectorMemoryUsage.offsetsByteCapacity - columnInfo.vectorMemoryUsage.offsetsBytesUsed;
  }

  private static long getRemainingDataCapacity(ColumnMemoryUsageInfo columnInfo) {
    return columnInfo.vectorMemoryUsage.dataByteCapacity - columnInfo.vectorMemoryUsage.dataBytesUsed;
  }

}
