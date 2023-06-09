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
package org.apache.drill.exec.record.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.drill.categories.VectorTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ExecTest;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetOptionBuilder;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DrillBuf;

@Category(VectorTest.class)
public class TestLoad extends ExecTest {
  private final DrillConfig drillConfig = DrillConfig.create();

  @Test
  public void testLoadValueVector() throws Exception {
    final BufferAllocator allocator = RootAllocatorFactory.newRoot(drillConfig);
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("ints", MinorType.INT)
        .add("chars", MinorType.VARCHAR)
        .addNullable("chars2", MinorType.VARCHAR);
    BatchSchema schema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    // Create vectors

    final List<ValueVector> vectors = createVectors(allocator, schema, 100);

    // Writeable batch now owns vector buffers

     final WritableBatch writableBatch = WritableBatch.getBatchNoHV(100, vectors, false);

     // Serialize the vectors

    final DrillBuf byteBuf = serializeBatch(allocator, writableBatch);

    // Batch loader does NOT take ownership of the serialized buffer

    final RecordBatchLoader batchLoader = new RecordBatchLoader(allocator);
    batchLoader.load(writableBatch.getDef(), byteBuf);

    // Release the serialized buffer.

    byteBuf.release();

    // TODO: Do actual validation

    assertEquals(100, batchLoader.getRecordCount());

    // Free the original vectors

    writableBatch.clear();

    // Free the deserialized vectors

    batchLoader.clear();

    // The allocator will verify that the frees were done correctly.

    allocator.close();
  }

  @Test
  public void testLoadValueVectorEmptyVarCharArray() throws Exception {
    try (BufferAllocator allocator = RootAllocatorFactory.newRoot(drillConfig)) {
      TupleMetadata schema = new SchemaBuilder()
          .addArray("chars", MinorType.VARCHAR)
          .build();

      ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
          .readerSchema(schema)
          .build();

      ResultSetLoader resultSetLoader = new ResultSetLoaderImpl(allocator, options);

      resultSetLoader.startBatch();
      RowSetLoader rowWriter = resultSetLoader.writer();

      rowWriter.addRow(new Object[]{null});

      VectorContainer harvest = resultSetLoader.harvest();

      // Create vectors
      List<ValueVector> vectors = StreamSupport.stream(harvest.spliterator(), false)
          .map(VectorWrapper::getValueVector)
          .collect(Collectors.toList());

      // Writeable batch now owns vector buffers
      WritableBatch writableBatch = WritableBatch.getBatchNoHV(1, vectors, false);

      // Serialize the vectors
      DrillBuf byteBuf = serializeBatch(allocator, writableBatch);

      // Batch loader does NOT take ownership of the serialized buffer
      RecordBatchLoader batchLoader = new RecordBatchLoader(allocator);
      batchLoader.load(writableBatch.getDef(), byteBuf);

      // Release the serialized buffer.
      byteBuf.release();

      assertEquals(1, batchLoader.getRecordCount());

      // Free the original vectors
      writableBatch.clear();

      // Free the deserialized vectors
      batchLoader.clear();
    }
  }

  // TODO: Replace this low-level code with RowSet usage once
  // DRILL-5657 is committed to master.

  private static List<ValueVector> createVectors(BufferAllocator allocator, BatchSchema schema, int i) {
    final List<ValueVector> vectors = new ArrayList<>();
    for (MaterializedField field : schema) {
      ValueVector v = TypeHelper.getNewVector(field, allocator);
      AllocationHelper.allocate(v, 100, 50);
      v.getMutator().generateTestData(100);
      vectors.add(v);
    }
    return vectors;
  }

  static DrillBuf serializeBatch(BufferAllocator allocator, WritableBatch writableBatch) {
    final ByteBuf[] byteBufs = writableBatch.getBuffers();
    int bytes = 0;
    for (ByteBuf buf : byteBufs) {
      bytes += buf.writerIndex();
    }
    final DrillBuf byteBuf = allocator.buffer(bytes);
    int index = 0;
    for (ByteBuf buf : byteBufs) {
      buf.readBytes(byteBuf, index, buf.writerIndex());
      index += buf.writerIndex();
    }
    byteBuf.writerIndex(bytes);
    return byteBuf;
  }

  /**
   * Test function to simulate loading a batch.
   *
   * @param allocator a memory allocator
   * @param batchLoader the batch loader under test
   * @param schema the schema of the new batch
   * @return false if the same schema, true if schema changed;
   * that is, whether the schema changed
   * @throws SchemaChangeException should not occur
   */

  private boolean loadBatch(BufferAllocator allocator,
      final RecordBatchLoader batchLoader,
      BatchSchema schema) throws SchemaChangeException {
    final List<ValueVector> vectors = createVectors(allocator, schema, 100);
    final WritableBatch writableBatch = WritableBatch.getBatchNoHV(100, vectors, false);
    final DrillBuf byteBuf = serializeBatch(allocator, writableBatch);
    boolean result = batchLoader.load(writableBatch.getDef(), byteBuf);
    byteBuf.release();
    writableBatch.clear();
    return result;
  }

  @Test
  public void testSchemaChange() throws SchemaChangeException {
    final BufferAllocator allocator = RootAllocatorFactory.newRoot(drillConfig);
    final RecordBatchLoader batchLoader = new RecordBatchLoader(allocator);

    // Initial schema: a: INT, b: VARCHAR
    // Schema change: N/A

    SchemaBuilder schemaBuilder1 = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR);
    BatchSchema schema1 = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder1)
        .build();
    {
      assertTrue(loadBatch(allocator, batchLoader, schema1));
      assertTrue(schema1.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Same schema
    // Schema change: No

    {
      assertFalse(loadBatch(allocator, batchLoader, schema1));
      assertTrue(schema1.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Reverse columns: b: VARCHAR, a: INT
    // Schema change: No

    {
      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("b", MinorType.VARCHAR)
          .add("a", MinorType.INT);
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertFalse(loadBatch(allocator, batchLoader, schema));

      // Potential bug: see DRILL-5828

      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Drop a column: a: INT
    // Schema change: Yes

    {
      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("a", MinorType.INT);
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Add a column: a: INT, b: VARCHAR, c: INT
    // Schema change: Yes

    {
      assertTrue(loadBatch(allocator, batchLoader, schema1));
      assertTrue(schema1.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();

      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("a", MinorType.INT)
          .add("b", MinorType.VARCHAR)
          .add("c", MinorType.INT);
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Change a column type: a: INT, b: VARCHAR, c: VARCHAR
    // Schema change: Yes

    {
      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("a", MinorType.INT)
          .add("b", MinorType.VARCHAR)
          .add("c", MinorType.VARCHAR);
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Empty schema
    // Schema change: Yes

    {
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(new SchemaBuilder())
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    batchLoader.clear();
    allocator.close();
  }

  @Test
  public void testMapSchemaChange() throws SchemaChangeException {
    final BufferAllocator allocator = RootAllocatorFactory.newRoot(drillConfig);
    final RecordBatchLoader batchLoader = new RecordBatchLoader(allocator);

    // Initial schema: a: INT, m: MAP{}

    SchemaBuilder schemaBuilder1 = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .resumeSchema();
    BatchSchema schema1 = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder1)
        .build();
    {
      assertTrue(loadBatch(allocator, batchLoader, schema1));
      assertTrue(schema1.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Same schema
    // Schema change: No

    {
      assertFalse(loadBatch(allocator, batchLoader, schema1));
      assertTrue(schema1.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Add column to map: a: INT, m: MAP{b: VARCHAR}
    // Schema change: Yes

    SchemaBuilder schemaBuilder2 = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addMap("m")
          .add("b", MinorType.VARCHAR)
          .resumeSchema();
    BatchSchema schema2 = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder2)
        .build();
    {
      assertTrue(loadBatch(allocator, batchLoader, schema2));
      assertTrue(schema2.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Same schema
    // Schema change: No

    {
      assertFalse(loadBatch(allocator, batchLoader, schema2));
      assertTrue(schema2.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Add column:  a: INT, m: MAP{b: VARCHAR, c: INT}
    // Schema change: Yes

    {
      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("a", MinorType.INT)
          .addMap("m")
            .add("b", MinorType.VARCHAR)
            .add("c", MinorType.INT)
            .resumeSchema();
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Drop a column:  a: INT, m: MAP{b: VARCHAR}
    // Schema change: Yes

    {
      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("a", MinorType.INT)
          .addMap("m")
            .add("b", MinorType.VARCHAR)
            .resumeSchema();
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Change type:  a: INT, m: MAP{b: INT}
    // Schema change: Yes

    {
      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("a", MinorType.INT)
          .addMap("m")
            .add("b", MinorType.INT)
            .resumeSchema();
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Empty map: a: INT, m: MAP{}

    {
      assertTrue(loadBatch(allocator, batchLoader, schema1));
      assertTrue(schema1.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    // Drop map: a: INT

    {
      SchemaBuilder schemaBuilder = new SchemaBuilder()
          .add("a", MinorType.INT);
      BatchSchema schema = new BatchSchemaBuilder()
          .withSchemaBuilder(schemaBuilder)
          .build();
      assertTrue(loadBatch(allocator, batchLoader, schema));
      assertTrue(schema.isEquivalent(batchLoader.getSchema()));
      batchLoader.getContainer().zeroVectors();
    }

    batchLoader.clear();
    allocator.close();
  }
}
