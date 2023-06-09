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
package org.apache.drill.exec.physical.impl.scan;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixture;
import org.apache.drill.exec.physical.impl.scan.ScanTestUtils.ScanFixtureBuilder;
import org.apache.drill.exec.physical.impl.scan.framework.BasicScanFactory;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework.ScanFrameworkBuilder;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test of the scan operator framework. Here the focus is on the
 * implementation of the scan operator itself. This operator is
 * based on a number of lower-level abstractions, each of which has
 * its own unit tests. To make this more concrete: review the scan
 * operator code paths. Each path should be exercised by one or more
 * of the tests here. If, however, the code path depends on the
 * details of another, supporting class, then tests for that class
 * appear elsewhere.
 */
public class BaseScanOperatorExecTest extends SubOperatorTest {
  static final Logger logger = LoggerFactory.getLogger(BaseScanOperatorExecTest.class);

  /**
   * Base class for the "mock" readers used in this test. The mock readers
   * follow the normal (enhanced) reader API, but instead of actually reading
   * from a data source, they just generate data with a known schema.
   * They also expose internal state such as identifying which methods
   * were actually called.
   */
  protected static abstract class BaseMockBatchReader implements ManagedReader<SchemaNegotiator> {
    protected boolean openCalled;
    protected boolean closeCalled;
    protected int startIndex;
    protected int batchCount;
    protected int batchLimit;
    protected ResultSetLoader tableLoader;

    protected void makeBatch() {
      RowSetLoader writer = tableLoader.writer();
      int offset = (batchCount - 1) * 20 + startIndex;
      writeRow(writer, offset + 10, "fred");
      writeRow(writer, offset + 20, "wilma");
    }

    protected void writeRow(RowSetLoader writer, int col1, String col2) {
      writer.start();
      if (writer.column(0) != null) {
        writer.scalar(0).setInt(col1);
      }
      if (writer.column(1) != null) {
        writer.scalar(1).setString(col2);
      }
      writer.save();
    }

    @Override
    public void close() {
      closeCalled = true;
    }
  }

  /**
   * Mock reader that pretends to have a schema at open time
   * like an HBase or JDBC reader.
   */
  protected static class MockEarlySchemaReader extends BaseMockBatchReader {

    @Override
    public boolean open(SchemaNegotiator schemaNegotiator) {
      openCalled = true;
      TupleMetadata schema = new SchemaBuilder()
          .add("a", MinorType.INT)
          .addNullable("b", MinorType.VARCHAR, 10)
          .buildSchema();
      schemaNegotiator.tableSchema(schema, true);
      tableLoader = schemaNegotiator.build();
      return true;
    }

    @Override
    public boolean next() {
      batchCount++;
      if (batchCount > batchLimit) {
        return false;
      }

      makeBatch();
      return true;
    }
  }

  protected TupleMetadata expectedSchema() {
    return new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.VARCHAR, 10)
        .buildSchema();
  }

  protected SingleRowSet makeExpected() {
    return makeExpected(0);
  }

  protected SingleRowSet makeExpected(int offset) {
    SingleRowSet expected = fixture.rowSetBuilder(expectedSchema())
        .addRow(offset + 10, "fred")
        .addRow(offset + 20, "wilma")
        .build();
    return expected;
  }

  protected void verifyBatch(int offset, VectorContainer output) {
    SingleRowSet expected = makeExpected(offset);
    RowSetUtilities.verify(expected, fixture.wrap(output));
  }

  public static class BaseScanFixtureBuilder extends ScanFixtureBuilder {

    public ScanFrameworkBuilder builder = new ScanFrameworkBuilder();
    public final List<ManagedReader<SchemaNegotiator>> readers = new ArrayList<>();

    public BaseScanFixtureBuilder() {
      super(fixture);
    }

    @Override
    public ScanFrameworkBuilder builder() { return builder; }

    public void addReader(ManagedReader<SchemaNegotiator> reader) {
      readers.add(reader);
    }

    public void addReaders(List<ManagedReader<SchemaNegotiator>> readers) {
      this.readers.addAll(readers);
    }

    @SuppressWarnings("unchecked")
    public void addReaders(ManagedReader<SchemaNegotiator>...readers) {
      for (ManagedReader<SchemaNegotiator> reader : readers) {
        addReader(reader);
      }
    }

    @Override
    public ScanFixture build() {
      builder.setReaderFactory(new BasicScanFactory(readers.iterator()));
      return super.build();
    }
  }

  @SafeVarargs
  public static BaseScanFixtureBuilder simpleBuilder(ManagedReader<SchemaNegotiator>...readers) {
    BaseScanFixtureBuilder builder = new BaseScanFixtureBuilder();
    builder.projectAll();
    builder.addReaders(readers);
    return builder;
  }

  @SafeVarargs
  public static ScanFixture simpleFixture(ManagedReader<SchemaNegotiator>...readers) {
    return simpleBuilder(readers).build();
  }
}
