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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.resultSet.PullResultSetReader;
import org.apache.drill.exec.physical.resultSet.ResultSetCopier;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.PullResultSetReaderImpl.UpstreamSource;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl.ResultSetOptions;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.physical.rowSet.RowSets;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector2Builder;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;

public class TestResultSetCopier extends SubOperatorTest {

  private static final TupleMetadata TEST_SCHEMA =
      new SchemaBuilder()
        .add("id", MinorType.INT)
        .add("name", MinorType.VARCHAR)
        .build();

  private static abstract class BaseDataGen implements UpstreamSource {
    protected int schemaVersion = 1;
    protected final ResultSetLoader rsLoader;
    protected VectorContainer batch;
    protected int batchCount;
    protected int rowCount;
    protected int batchSize;
    protected int batchLimit;

    public BaseDataGen(TupleMetadata schema, int batchSize, int batchLimit) {
      ResultSetOptions options = new ResultSetOptionBuilder()
          .readerSchema(schema)
          .vectorCache(new ResultVectorCacheImpl(fixture.allocator()))
          .build();
      rsLoader = new ResultSetLoaderImpl(fixture.allocator(), options);
      this.batchSize = batchSize;
      this.batchLimit = batchLimit;
    }

    @Override
    public int schemaVersion() { return schemaVersion; }

    @Override
    public VectorContainer batch() { return batch; }

    @Override
    public boolean next() {
      if (batchCount >= batchLimit) {
        return false;
      }
      makeBatch();
      return true;
    }

    protected abstract void makeBatch();

    @Override
    public SelectionVector2 sv2() { return null; }

    @Override
    public void release() {
      if (batch != null) {
        batch.zeroVectors();
      }
      SelectionVector2 sv2 = sv2();
      if (sv2 != null) {
        sv2.clear();
      }
    }
  }

  private static class DataGen extends BaseDataGen {

    public DataGen() {
      this(3, 1);
     }

    public DataGen(int batchSize, int batchLimit) {
      super(TEST_SCHEMA, batchSize, batchLimit);
    }

    @Override
    protected void makeBatch() {
      rsLoader.startBatch();
      for (int i = 0; i < batchSize; i++) {
        rowCount++;
        rsLoader.writer().addRow(rowCount, "Row " + rowCount);
      }
      batch = rsLoader.harvest();
      batchCount++;
    }
  }

  public static class SchemaChangeGen extends DataGen {

    int schema1Limit;

    public SchemaChangeGen(int batchSize, int batchLimit, int schema1Limit) {
      super(batchSize, batchLimit);
      this.schema1Limit = schema1Limit;
    }

    public SchemaChangeGen(int schema1Limit) {
      super(3, 3);
      this.schema1Limit = schema1Limit;
    }

    public SchemaChangeGen() {
      this(2);
    }

    public TupleMetadata schema2() {
      return new SchemaBuilder()
          .add("id", MinorType.INT)
          .add("name", MinorType.VARCHAR)
          .add("amount", MinorType.INT)
          .build();
    }

    @Override
    protected void makeBatch() {
      if (batchCount < schema1Limit) {
        super.makeBatch();
      } else if (batchCount == schema1Limit) {
        evolveSchema();
        makeBatch2();
      } else {
        makeBatch2();
      }
    }

    public void makeBatch2() {
      rsLoader.startBatch();
      for (int i = 0; i < batchSize; i++) {
        rowCount++;
        rsLoader.writer().addRow(rowCount, "Row " + rowCount, rowCount * 10);
      }
      batch = rsLoader.harvest();
      batchCount++;
    }

    public void evolveSchema() {
      rsLoader.writer().addColumn(MetadataUtils.newScalar("amount", MinorType.INT, DataMode.REQUIRED));
      schemaVersion = 2;
    }
  }

  private static class NullableGen extends BaseDataGen {

    public NullableGen() {
      super(new SchemaBuilder()
          .add("id", MinorType.INT)
          .addNullable("name", MinorType.VARCHAR)
          .addNullable("amount", MinorType.INT)
          .build(),
          10, 1);
    }

    @Override
    protected void makeBatch() {
      rsLoader.startBatch();
      RowSetLoader writer = rsLoader.writer();
      for (int i = 0; i < batchSize; i++) {
        rowCount++;
        writer.start();
        writer.scalar(0).setInt(rowCount);
        if (i % 2 == 0) {
          writer.scalar(1).setString("Row " + rowCount);
        }
        if (i % 3 == 0) {
          writer.scalar(2).setInt(rowCount * 10);
        }
        writer.save();
      }
      batch = rsLoader.harvest();
      batchCount++;
    }
  }

  private static class ArrayGen extends BaseDataGen {

    public ArrayGen() {
      super(new SchemaBuilder()
          .add("id", MinorType.INT)
          .addArray("name", MinorType.VARCHAR)
          .build(),
          3, 1);
    }

    @Override
    protected void makeBatch() {
      rsLoader.startBatch();
      RowSetLoader writer = rsLoader.writer();
      ArrayWriter aw = writer.array(1);
      for (int i = 0; i < batchSize; i++) {
        rowCount++;
        writer.start();
        writer.scalar(0).setInt(rowCount);
        int n = i % 3;
        for (int j = 0; j < n; j++) {
          aw.scalar().setString("Row " + rowCount + "." + j);
        }
        writer.save();
      }
      batch = rsLoader.harvest();
      batchCount++;
    }
  }

  private static class MapGen extends BaseDataGen {

    public MapGen() {
      super(new SchemaBuilder()
          .add("id", MinorType.INT)
          .addMapArray("map")
            .add("name", MinorType.VARCHAR)
            .add("amount", MinorType.INT)
            .resumeSchema()
          .build(),
          3, 1);
    }

    @Override
    protected void makeBatch() {
      rsLoader.startBatch();
      RowSetLoader writer = rsLoader.writer();
      ArrayWriter aw = writer.array(1);
      TupleWriter mw = aw.entry().tuple();
      for (int i = 0; i < batchSize; i++) {
        rowCount++;
        writer.start();
        writer.scalar(0).setInt(rowCount);
        int n = i % 3;
        for (int j = 0; j < n; j++) {
          mw.scalar(0).setString("Row " + rowCount + "." + j);
          mw.scalar(1).setInt(rowCount * 100 + j);
          aw.save();
        }
        writer.save();
      }
      batch = rsLoader.harvest();
      batchCount++;
    }
  }

  public static class FilteredGen extends DataGen {

    SelectionVector2 sv2;

    public FilteredGen() {
      super(10, 1);
    }

    @Override
    protected void makeBatch() {
      super.makeBatch();
      makeSv2();
    }

    // Pick out every other record, in descending
    // order.
    private void makeSv2() {
      SelectionVector2Builder sv2Builder =
          new SelectionVector2Builder(fixture.allocator(), batch.getRecordCount());
      for (int i = 0; i < 5; i++) {
        sv2Builder.setNext(10 - 2 * i - 1);
      }
      sv2 =  sv2Builder.harvest(batch);
      batch.buildSchema(SelectionVectorMode.TWO_BYTE);
    }

    @Override
    public SelectionVector2 sv2() { return sv2; }
  }

  private ResultSetCopierImpl newCopier(UpstreamSource source) {
    PullResultSetReader reader = new PullResultSetReaderImpl(source);
    return new ResultSetCopierImpl(fixture.allocator(), reader);
  }

  private ResultSetCopierImpl newCopier(UpstreamSource source, ResultSetOptionBuilder outputOptions) {
    PullResultSetReader reader = new PullResultSetReaderImpl(source);
    return new ResultSetCopierImpl(fixture.allocator(), reader, outputOptions);
  }

  @Test
  public void testBasics() {

    DataGen dataGen = new DataGen();
    ResultSetCopier copier = newCopier(dataGen);

    // Nothing should work yet
    try {
      copier.copyAllRows();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }
    try {
      copier.harvest();
      fail();
    } catch (IllegalStateException e) {
      // Expected
    }

    // Predicates should work
    assertFalse(copier.isCopyPending());
    assertFalse(copier.hasOutputRows());
    assertFalse(copier.isOutputFull());

    // Define a schema and start an output batch.
    copier.startOutputBatch();
    assertFalse(copier.isCopyPending());
    assertFalse(copier.hasOutputRows());
    assertFalse(copier.isOutputFull());

    // Provide an input batch
    assertTrue(copier.nextInputBatch());
    assertFalse(copier.isCopyPending());
    assertFalse(copier.hasOutputRows());
    assertFalse(copier.isOutputFull());

    // Now can do some actual copying
    while (copier.copyNextRow()) {
      // empty
    }
    assertFalse(copier.isCopyPending());
    assertTrue(copier.hasOutputRows());
    assertFalse(copier.isOutputFull());

    // Get and verify the output batch
    // (Does not free the input batch, we reuse it
    // in the verify step below.)
    RowSet result = fixture.wrap(copier.harvest());
    new RowSetComparison(fixture.wrap(dataGen.batch()))
      .verifyAndClear(result);

    // No more input
    copier.startOutputBatch();
    assertFalse(copier.nextInputBatch());

    // OK to try multiple times
    assertFalse(copier.nextInputBatch());

    // Copier will release the input batch
    copier.close();
  }

  @Test
  public void testImmediateClose() {

    ResultSetCopier copier = newCopier(new DataGen());

    // Close OK before things got started
    copier.close();

    // Second close is benign
    copier.close();
  }

  @Test
  public void testCloseBeforeSchema() {

    ResultSetCopier copier = newCopier(new DataGen());

    // Start batch, no data yet.
    copier.startOutputBatch();

    // Close OK before things data arrives
    copier.close();

    // Second close is benign
    copier.close();
  }

  @Test
  public void testCloseWithData() {

    ResultSetCopier copier = newCopier(new DataGen());

    // Start batch, with data.
    copier.startOutputBatch();
    copier.nextInputBatch();
    copier.copyNextRow();

    // Close OK with input and output batch allocated.
    copier.close();

    // Second close is benign
    copier.close();
  }

  /**
   * Test merging multiple batches from the same input
   * source; all batches share the same vectors, hence
   * implicitly the same schema.
   * <p>
   * This copier does not support merging from multiple
   * streams.
   */
  @Test
  public void testMerge() {
    ResultSetCopier copier = newCopier(new DataGen(3, 5));
    copier.startOutputBatch();

    for (int i = 0; i < 5; i++) {
      assertTrue(copier.nextInputBatch());
      assertFalse(copier.isOutputFull());
      copier.copyAllRows();
      assertFalse(copier.isOutputFull());
      assertFalse(copier.isCopyPending());
    }
    assertFalse(copier.nextInputBatch());
    RowSet result = fixture.wrap(copier.harvest());

    // Verify with single batch with all rows
    DataGen dataGen = new DataGen(15, 1);
    dataGen.next();
    RowSet expected = RowSets.wrap(dataGen.batch());
    RowSetUtilities.verify(expected, result);

    copier.close();
  }

  @Test
  public void testMultiOutput() {

    // Equivalent of operator start() method.
    DataGen dataGen = new DataGen(15, 2);
    ResultSetOptionBuilder options = new ResultSetOptionBuilder()
        .rowCountLimit(12);
    ResultSetCopier copier = newCopier(dataGen, options);

    // Equivalent of an entire operator run
    DataGen validatorGen = new DataGen(12, 2);
    int outputCount = 0;
    while (true) {

      // Equivalent of operator next() method
      copier.startOutputBatch();
      while (! copier.isOutputFull()) {
        if (!copier.nextInputBatch()) {
          break;
        }
        copier.copyAllRows();
      }
      if (!copier.hasOutputRows()) {
        break;
      }

      // Equivalent of sending downstream
      RowSet result = fixture.wrap(copier.harvest());

      validatorGen.next();
      RowSet expected = RowSets.wrap(validatorGen.batch());
      RowSetUtilities.verify(expected, result, result.rowCount());
      outputCount++;
    }

    // Ensure more than one output batch.
    assertTrue(outputCount > 1);

    // Ensure all rows generated.
    assertEquals(30, dataGen.rowCount);

    // Simulate operator close();
    copier.close();
  }

  @Test
  public void testCopyRecord() {
    ResultSetCopier copier = newCopier(new DataGen(3, 2));
    copier.startOutputBatch();

    copier.nextInputBatch();
    copier.copyRow(2);
    copier.copyRow(0);
    copier.copyRow(1);

    copier.nextInputBatch();
    copier.copyRow(1);
    copier.copyRow(0);
    copier.copyRow(2);

    assertFalse(copier.nextInputBatch());

    RowSet expected = new RowSetBuilder(fixture.allocator(), TEST_SCHEMA)
        .addRow(3, "Row 3")
        .addRow(1, "Row 1")
        .addRow(2, "Row 2")
        .addRow(5, "Row 5")
        .addRow(4, "Row 4")
        .addRow(6, "Row 6")
        .build();
    RowSetUtilities.verify(expected, fixture.wrap(copier.harvest()));

    copier.close();
  }

  @Test
  public void testSchemaChange() {
    ResultSetCopier copier = newCopier(new SchemaChangeGen(3, 4, 2));

    // Copy first batch with first schema
    copier.startOutputBatch();
    assertTrue(copier.nextInputBatch());
    copier.copyAllRows();
    assertFalse(copier.isOutputFull());

    // Second, same schema
    assertTrue(copier.nextInputBatch());
    copier.copyAllRows();
    assertFalse(copier.isOutputFull());

    // Plenty of room. But, change the schema.
    assertTrue(copier.nextInputBatch());
    assertTrue(copier.isOutputFull());

    // Must harvest partial output
    RowSet result = fixture.wrap(copier.harvest());
    SchemaChangeGen verifierGen = new SchemaChangeGen(6, 2, 1);
    verifierGen.next();
    RowSet expected = RowSets.wrap(verifierGen.batch());
    RowSetUtilities.verify(expected, result);

    // Start a new batch, implicitly complete pending copy
    copier.startOutputBatch();
    copier.copyAllRows();

    // Add one more of second schema
    assertTrue(copier.nextInputBatch());
    copier.copyAllRows();
    assertFalse(copier.isOutputFull());

    result = fixture.wrap(copier.harvest());
    verifierGen.next();
    expected = RowSets.wrap(verifierGen.batch());
    RowSetUtilities.verify(expected, result);
    assertFalse(copier.isCopyPending());

    copier.close();
  }

  // TODO: Test with two consecutive schema changes in
  // same input batch: once with rows pending, another without.

  @Test
  public void testSV2() {
    ResultSetCopier copier = newCopier(new FilteredGen());

    copier.startOutputBatch();
    assertTrue(copier.nextInputBatch());
    copier.copyAllRows();

    RowSet expected = new RowSetBuilder(fixture.allocator(), TEST_SCHEMA)
        .addRow(10, "Row 10")
        .addRow(8, "Row 8")
        .addRow(6, "Row 6")
        .addRow(4, "Row 4")
        .addRow(2, "Row 2")
        .build();
    RowSet result = fixture.wrap(copier.harvest());
    RowSetUtilities.verify(expected, result);

    copier.close();
  }

  @Test
  public void testSV4() {
    // TODO
  }

  @Test
  public void testNullable() {
    ResultSetCopier copier = newCopier(new NullableGen());
    copier.startOutputBatch();

    copier.nextInputBatch();
    copier.copyAllRows();

    RowSet result = fixture.wrap(copier.harvest());
    NullableGen verifierGen = new NullableGen();
    verifierGen.next();
    RowSet expected = RowSets.wrap(verifierGen.batch());
    RowSetUtilities.verify(expected, result);

    copier.close();
  }

  @Test
  public void testArrays() {
    ResultSetCopier copier = newCopier(new ArrayGen());
    copier.startOutputBatch();

    copier.nextInputBatch();
    copier.copyAllRows();

    RowSet result = fixture.wrap(copier.harvest());
    ArrayGen verifierGen = new ArrayGen();
    verifierGen.next();
    RowSet expected = RowSets.wrap(verifierGen.batch());
    RowSetUtilities.verify(expected, result);

    copier.close();
  }

  @Test
  public void testMaps() {
    ResultSetCopier copier = newCopier(new MapGen());
    copier.startOutputBatch();

    copier.nextInputBatch();
    copier.copyAllRows();

    RowSet result = fixture.wrap(copier.harvest());
    MapGen verifierGen = new MapGen();
    verifierGen.next();
    RowSet expected = RowSets.wrap(verifierGen.batch());
    RowSetUtilities.verify(expected, result);

    copier.close();
  }

  @Test
  public void testUnions() {
    // TODO
  }

  @Test
  public void testOverflow() {
    // TODO
  }
}
