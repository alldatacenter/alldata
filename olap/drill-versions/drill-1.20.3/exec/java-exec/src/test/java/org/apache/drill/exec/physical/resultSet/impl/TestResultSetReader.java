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
package org.apache.drill.exec.physical.resultSet.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.PullResultSetReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.PullResultSetReaderImpl.UpstreamSource;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl.ResultSetOptions;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;

public class TestResultSetReader extends SubOperatorTest {

  private static final TupleMetadata SCHEMA1 = new SchemaBuilder()
      .add("id", MinorType.INT)
      .add("name", MinorType.VARCHAR)
      .build();
  private static final TupleMetadata SCHEMA2 = new SchemaBuilder()
      .addAll(SCHEMA1)
      .add("amount", MinorType.INT)
      .build();

  public static class BatchGenerator implements UpstreamSource {

    private enum State { SCHEMA1, SCHEMA2 };

    private final ResultSetLoader rsLoader;
    private VectorContainer batch;
    private int schemaVersion;
    private State state;
    private int batchCount;
    private int rowCount;
    private final int schema1Count;
    private final int schema2Count;
    private final int batchSize;

    public BatchGenerator(int batchSize, int schema1Count, int schema2Count) {
      ResultSetOptions options = new ResultSetOptionBuilder()
          .readerSchema(SCHEMA1)
          .vectorCache(new ResultVectorCacheImpl(fixture.allocator()))
          .build();
      this.rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
      this.state = State.SCHEMA1;
      this.batchSize = batchSize;
      this.schemaVersion = 1;
      this.schema1Count = schema1Count;
      this.schema2Count = schema2Count;
    }

    public void batch1() {
      Preconditions.checkState(state == State.SCHEMA1);
      rsLoader.startBatch();
      RowSetLoader writer = rsLoader.writer();
      for (int i = 0; i < batchSize; i++) {
        rowCount++;
        writer.start();
        writer.scalar("id").setInt(rowCount);
        writer.scalar("name").setString("Row" + rowCount);
        writer.save();
      }
      batch = rsLoader.harvest();
      batchCount++;
    }

    public void batch2() {
      RowSetLoader writer = rsLoader.writer();
      if (state == State.SCHEMA1) {
        writer.addColumn(SCHEMA2.metadata("amount"));
        state = State.SCHEMA2;
        schemaVersion++;
      }
      rsLoader.startBatch();
      for (int i = 0; i < batchSize; i++) {
        rowCount++;
        writer.start();
        writer.scalar("id").setInt(rowCount);
        writer.scalar("name").setString("Row" + rowCount);
        writer.scalar("amount").setInt(rowCount * 10);
        writer.save();
      }
      batch = rsLoader.harvest();
      batchCount++;
    }

    public void close() {
      rsLoader.close();
    }

    @Override
    public boolean next() {
      if (batchCount == schema1Count + schema2Count) {
        return false;
      }
      if (batchCount < schema1Count) {
        batch1();
      } else {
        batch2();
      }
      return true;
    }

    @Override
    public int schemaVersion() { return schemaVersion; }

    @Override
    public VectorContainer batch() { return batch; }

    @Override
    public SelectionVector2 sv2() { return null; }

    @Override
    public void release() {
      if (batch != null) {
        batch.zeroVectors();
      }
    }
  }

  @Test
  public void testBasics() {
    PullResultSetReader rsReader = new PullResultSetReaderImpl(
        new BatchGenerator(10, 2, 1));

    // Start state
    try {
      rsReader.reader();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Ask for schema. Does an implicit next.
    assertEquals(SCHEMA1, rsReader.schema());
    assertEquals(1, rsReader.schemaVersion());

    // Move to the first batch.
    // (Don't need to do a full reader test, that is already done
    // elsewhere.)
    assertTrue(rsReader.next());
    assertEquals(1, rsReader.schemaVersion());
    RowSetReader reader1;
    {
      RowSetReader reader = rsReader.reader();
      reader1 = reader;
      assertTrue(reader.next());
      assertEquals(1, reader.scalar("id").getInt());
      assertEquals("Row1", reader.scalar("name").getString());
    }

    // Second batch, same schema.
    assertTrue(rsReader.next());
    assertEquals(1, rsReader.schemaVersion());
    {
      RowSetReader reader = rsReader.reader();
      assertSame(reader1, reader);
      reader1 = reader;
      assertTrue(reader.next());
      assertEquals(11, reader.scalar("id").getInt());
      assertEquals("Row11", reader.scalar("name").getString());
    }

    // Batch with new schema
    assertTrue(rsReader.next());
    assertEquals(2, rsReader.schemaVersion());
    {
      RowSetReader reader = rsReader.reader();
      assertNotSame(reader1, reader);
      reader1 = reader;
      assertTrue(reader.next());
      assertEquals(21, reader.scalar("id").getInt());
      assertEquals("Row21", reader.scalar("name").getString());
      assertEquals(210, reader.scalar("amount").getInt());
    }

    assertFalse(rsReader.next());
    rsReader.close();
  }

  @Test
  public void testCloseAtStart() {
    PullResultSetReader rsReader = new PullResultSetReaderImpl(
        new BatchGenerator(10, 2, 1));

    // Close OK in start state
    rsReader.close();

    // Second close OK
    rsReader.close();
  }

  @Test
  public void testCloseDuringRead() {
    PullResultSetReader rsReader = new PullResultSetReaderImpl(
        new BatchGenerator(10, 2, 1));

    // Move to first batch
    assertTrue(rsReader.next());

    // Close OK in start state
    rsReader.close();

    // Second close OK
    rsReader.close();
  }

  @Test
  public void testCloseAfterNext() {
    PullResultSetReader rsReader = new PullResultSetReaderImpl(
        new BatchGenerator(10, 2, 1));

    // Move to first batch
    assertTrue(rsReader.next());

    // Close OK in start state
    rsReader.close();

    // Second close OK
    rsReader.close();
  }
}
