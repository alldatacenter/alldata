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
package org.apache.drill.exec.physical.impl.svremover;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.MockRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.SchemaChangeCallBack;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.OperatorFixture;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractGenericCopierTest extends BaseTest {
  @Rule
  public final BaseDirTestWatcher baseDirTestWatcher = new BaseDirTestWatcher();

  @Test
  public void testCopyRecords() throws Exception {
    try (OperatorFixture operatorFixture = new OperatorFixture.Builder(baseDirTestWatcher).build()) {
      final BufferAllocator allocator = operatorFixture.allocator();
      final BatchSchema batchSchema = createTestSchema(BatchSchema.SelectionVectorMode.NONE);
      final RowSet srcRowSet = createSrcRowSet(allocator);
      final VectorContainer destContainer = new VectorContainer(allocator, batchSchema);

      destContainer.setRecordCount(0);
      final RowSet expectedRowSet = createExpectedRowset(allocator);

      MockRecordBatch mockRecordBatch = null;

      try {
        mockRecordBatch = new MockRecordBatch.Builder().
          sendData(srcRowSet).
          build(operatorFixture.getFragmentContext());
        mockRecordBatch.next();
        final Copier copier = createCopier(mockRecordBatch, destContainer, null);
        copier.copyRecords(0, 3);

        new RowSetComparison(expectedRowSet).verify(DirectRowSet.fromContainer(destContainer));
      } finally {
        if (mockRecordBatch != null) {
          mockRecordBatch.close();
        }

        srcRowSet.clear();
        destContainer.clear();
        expectedRowSet.clear();
      }
    }
  }

  @Test
  public void testAppendRecords() throws Exception {
    try (OperatorFixture operatorFixture = new OperatorFixture.Builder(baseDirTestWatcher).build()) {
      final BufferAllocator allocator = operatorFixture.allocator();
      final BatchSchema batchSchema = createTestSchema(BatchSchema.SelectionVectorMode.NONE);
      final RowSet srcRowSet = createSrcRowSet(allocator);
      final VectorContainer destContainer = new VectorContainer(allocator, batchSchema);

      AbstractCopier.allocateOutgoing(destContainer, 3);

      destContainer.setRecordCount(0);
      final RowSet expectedRowSet = createExpectedRowset(allocator);

      MockRecordBatch mockRecordBatch = null;

      try {
        mockRecordBatch = new MockRecordBatch.Builder().
          sendData(srcRowSet).
          build(operatorFixture.getFragmentContext());
        mockRecordBatch.next();
        final Copier copier = createCopier(mockRecordBatch, destContainer, null);
        copier.appendRecord(0);
        copier.appendRecords(1, 2);

        new RowSetComparison(expectedRowSet).verify(DirectRowSet.fromContainer(destContainer));
      } finally {
        if (mockRecordBatch != null) {
          mockRecordBatch.close();
        }

        srcRowSet.clear();
        destContainer.clear();
        expectedRowSet.clear();
      }
    }
  }

  public abstract RowSet createSrcRowSet(BufferAllocator allocator) throws SchemaChangeException;

  public Copier createCopier(RecordBatch incoming, VectorContainer outputContainer,
                                      SchemaChangeCallBack callback) {
    return GenericCopierFactory.createAndSetupCopier(incoming, outputContainer, callback);
  }

  public static Object[] row1() {
    return new Object[]{110, "green", new float[]{5.5f, 2.3f}, new String[]{"1a", "1b"}};
  }

  public static Object[] row2() {
    return new Object[]{109, "blue", new float[]{1.5f}, new String[]{"2a"}};
  }

  public static Object[] row3() {
    return new Object[]{108, "red", new float[]{-11.1f, 0.0f, .5f}, new String[]{"3a", "3b", "3c"}};
  }

  public static Object[] row4() {
    return new Object[]{107, "yellow", new float[]{4.25f, 1.25f}, new String[]{}};
  }

  public static Object[] row5() {
    return new Object[]{106, "black", new float[]{.75f}, new String[]{"4a"}};
  }

  public RowSet createExpectedRowset(BufferAllocator allocator) {
    return new RowSetBuilder(allocator, createTestSchema(BatchSchema.SelectionVectorMode.NONE))
      .addRow(row1())
      .addRow(row2())
      .addRow(row3())
      .build();
  }

  protected BatchSchema createTestSchema(BatchSchema.SelectionVectorMode mode) {
    MaterializedField colA = MaterializedField.create("colA", Types.required(TypeProtos.MinorType.INT));
    MaterializedField colB = MaterializedField.create("colB", Types.required(TypeProtos.MinorType.VARCHAR));
    MaterializedField colC = MaterializedField.create("colC", Types.repeated(TypeProtos.MinorType.FLOAT4));
    MaterializedField colD = MaterializedField.create("colD", Types.repeated(TypeProtos.MinorType.VARCHAR));

    SchemaBuilder schemaBuilder = new SchemaBuilder().add(colA)
      .add(colB)
      .add(colC)
      .add(colD);
    return new BatchSchemaBuilder()
      .withSchemaBuilder(schemaBuilder)
      .withSVMode(mode)
      .build();
  }
}
