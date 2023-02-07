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
package org.apache.drill.exec.physical.impl.scan.v3.lifecycle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.exceptions.UserException.Builder;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.base.AbstractSubScan;
import org.apache.drill.exec.physical.impl.scan.RowBatchReader;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.v3.ReaderFactory;
import org.apache.drill.exec.physical.impl.scan.v3.ScanLifecycleBuilder;
import org.apache.drill.exec.physical.impl.scan.v3.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTestScanLifecycle extends SubOperatorTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseTestScanLifecycle.class);

  public static class DummySubScan extends AbstractSubScan {

    public DummySubScan() {
      super("fake-user");
    }

    @Override
    public String getOperatorType() { return "DUMMY_SUB_SCAN"; }
  }

  protected static abstract class SingleReaderFactory implements ReaderFactory<SchemaNegotiator> {

    private int counter;

    @Override
    public boolean hasNext() {
      return counter == 0;
    }
  }

  protected static abstract class TwoReaderFactory implements ReaderFactory<SchemaNegotiator> {

    private int counter;

    @Override
    public ManagedReader next(SchemaNegotiator negotiator) {
      counter++;
      switch (counter) {
        case 1:
          return firstReader(negotiator);
        case 2:
          return secondReader(negotiator);
        default:
          return null;
      }
    }

    public abstract ManagedReader firstReader(SchemaNegotiator negotiator);
    public abstract ManagedReader secondReader(SchemaNegotiator negotiator);

    @Override
    public boolean hasNext() {
      return counter < 2;
    }

    public int count() { return counter; }
  }

  public static final TupleMetadata SCHEMA = new SchemaBuilder()
      .add("a", MinorType.INT)
      .addNullable("b", MinorType.VARCHAR)
      .build();

  /**
   * Base class for the "mock" readers used in this test. The mock readers
   * follow the normal (enhanced) reader API, but instead of actually reading
   * from a data source, they just generate data with a known schema.
   * They also expose internal state such as identifying which methods
   * were actually called.
   */
  protected static abstract class BaseMockBatchReader implements ManagedReader {
    protected int startIndex;
    protected int batchCount;
    protected int batchLimit;
    protected ResultSetLoader tableLoader;

    public BaseMockBatchReader(int batchLimit) {
      this.batchLimit = batchLimit;
    }

    protected void makeBatch() {
      RowSetLoader writer = tableLoader.writer();
      int offset = (batchCount - 1) * 20 + startIndex;
      writer.addRow(offset + 10, "fred");
      writer.addRow(offset + 20, "wilma");
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

    @Override
    public void close() {}
  }

  /**
   * Mock reader with no data or schema, indicated by an early EOF
   * exception.
   */
  protected static class NoDataReader extends BaseMockBatchReader {

    public NoDataReader(SchemaNegotiator sn) throws EarlyEofException {
      super(0);
      throw new EarlyEofException();
    }

    @Override
    public boolean next() { return false; }
  }

  protected static class MockEmptySchemaReader implements ManagedReader {

    private final ResultSetLoader tableLoader;

    public MockEmptySchemaReader(SchemaNegotiator negotiator) {
      negotiator.tableSchema(new TupleSchema(), true);
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 0) {
        return false;
      }
      RowSetLoader writer = tableLoader.writer();
      writer.addRow();
      writer.addRow();
      return true;
    }

    @Override
    public void close() { }
  }

  /**
   * "Early schema" refers to the case in which the reader can provide a schema
   * when the reader is opened. Examples: CSV, HBase, MapR-DB binary, JDBC.
   */
  protected static class MockEarlySchemaReader extends BaseMockBatchReader {

    public MockEarlySchemaReader(SchemaNegotiator negotiator, int batchCount) {
      super(batchCount);
      negotiator.tableSchema(SCHEMA);
      negotiator.schemaIsComplete(true);
      tableLoader = negotiator.build();
    }
  }

  protected static class MockLateSchemaReader extends BaseMockBatchReader {

    public MockLateSchemaReader(SchemaNegotiator negotiator, int batchCount) {
      super(batchCount);
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (batchLimit == 0) {
        return false;
      }
      if (batchCount == 0) {
        RowSetLoader rowSet = tableLoader.writer();
        rowSet.addColumn(SCHEMA.metadata(0));
        rowSet.addColumn(SCHEMA.metadata(1));
      }
      return super.next();
    }
  }

  protected static class MockSingleColReader implements ManagedReader {

    private final ResultSetLoader tableLoader;

    public MockSingleColReader(SchemaNegotiator negotiator) {
      negotiator.tableSchema(new SchemaBuilder()
        .add("a", MinorType.INT)
        .build());
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 0) {
        return false;
      }
      RowSetLoader rowSet = tableLoader.writer();
      rowSet.addSingleCol(101);
      rowSet.addSingleCol(102);
      return true;
    }

    @Override
    public void close() { }
  }

  protected static class MockThreeColReader implements ManagedReader {

    public static final TupleMetadata READER_SCHEMA = new SchemaBuilder()
        .addAll(SCHEMA)
        .add("c", MinorType.BIGINT)
        .build();

    private final ResultSetLoader tableLoader;

    public MockThreeColReader(SchemaNegotiator negotiator) {
      negotiator.tableSchema(READER_SCHEMA);
      negotiator.schemaIsComplete(true);
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 0) {
        return false;
      }
      RowSetLoader rowSet = tableLoader.writer();
      rowSet.addRow(101, "wilma", 1001);
      rowSet.addRow(102, "betty", 1002);
      return true;
    }

    @Override
    public void close() { }
  }

  public static final TupleMetadata CONFLICT_SCHEMA = new SchemaBuilder()
      .add("a", MinorType.INT)
      .addNullable("b", MinorType.BIGINT)
      .build();

  protected static class MockEarlySchemaTypeConflictReader implements ManagedReader {

    private final ResultSetLoader tableLoader;

    public MockEarlySchemaTypeConflictReader(SchemaNegotiator negotiator) {
      negotiator.tableSchema(CONFLICT_SCHEMA);
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 0) {
        return false;
      }
      RowSetLoader rowSet = tableLoader.writer();
      rowSet.addRow(101, 1001);
      rowSet.addRow(102, 1002);
      return true;
    }

    @Override
    public void close() { }
  }

  protected static class MockLateSchemaTypeConflictReader implements ManagedReader {

    private final ResultSetLoader tableLoader;

    public MockLateSchemaTypeConflictReader(SchemaNegotiator negotiator) {
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 0) {
        return false;
      }
      RowSetLoader rowSet = tableLoader.writer();
      if (tableLoader.batchCount() == 0) {
        rowSet.addColumn(CONFLICT_SCHEMA.metadata(0).copy());
        rowSet.addColumn(CONFLICT_SCHEMA.metadata(1).copy());
      }
      rowSet.addRow(101, 1001);
      rowSet.addRow(102, 1002);
      return true;
    }

    @Override
    public void close() { }
  }

  protected static class MockModeConflictReader implements ManagedReader {

    public static final TupleMetadata READER_SCHEMA = new SchemaBuilder()
        .add("a", MinorType.INT)
        .add("b", MinorType.VARCHAR) // Nullable in base reader
        .build();

    private final ResultSetLoader tableLoader;

    public MockModeConflictReader(SchemaNegotiator negotiator) {
      negotiator.tableSchema(READER_SCHEMA);
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 0) {
        return false;
      }
      RowSetLoader rowSet = tableLoader.writer();
      rowSet.addRow(101, "wilma");
      rowSet.addRow(102, "betty");
      return true;
    }

    @Override
    public void close() { }
  }

  protected static class MockReorderedReader implements ManagedReader {

    public static final TupleMetadata READER_SCHEMA = new SchemaBuilder()
        .add(SCHEMA.metadata(1).copy())
        .add(SCHEMA.metadata(0).copy())
        .build();

    private final ResultSetLoader tableLoader;

    public MockReorderedReader(SchemaNegotiator negotiator) {
      negotiator.tableSchema(READER_SCHEMA);
      tableLoader = negotiator.build();
    }

    @Override
    public boolean next() {
      if (tableLoader.batchCount() > 0) {
        return false;
      }
      RowSetLoader writer = tableLoader.writer();
      writer.addRow("barney", 30);
      writer.addRow("betty", 40);
      return true;
    }

    @Override
    public void close() { }
  }

  protected static class FailingReader implements ManagedReader {

    public final String failType;
    public final CustomErrorContext ec;

    public FailingReader(SchemaNegotiator sn, String failType) {
      this.failType = failType;
      this.ec = new ChildErrorContext(sn.parentErrorContext()) {
        @Override
        public void addContext(Builder builder) {
          super.addContext(builder);
          builder.addContext("My custom context");
        }
      };
      sn.setErrorContext(ec);
      if (failType.equals("ctor")) {
        throw new IllegalStateException("Oops ctor");
      }
      if (failType.equals("ctor-u")) {
        throw UserException.dataReadError()
          .message("Oops ctor")
          .addContext(ec)
          .build(logger);
      }
      sn.build();
    }

    @Override
    public boolean next() {
      if (failType.equals("next")) {
        throw new IllegalStateException("Oops next");
      }
      if (failType.equals("next-u")) {
        throw UserException.dataReadError()
          .message("Oops next")
          .addContext(ec)
          .build(logger);
      }
      return false;
    }

    @Override
    public void close() {
      if (failType.equals("close")) {
        throw new IllegalStateException("Oops close");
      }
      if (failType.equals("close-u")) {
        throw UserException.dataReadError()
          .message("Oops close")
          .addContext(ec)
          .build(logger);
      }
    }
  }

  protected ScanLifecycle buildScan(ScanLifecycleBuilder builder) {
    return builder.build(
        fixture.operatorContext(new DummySubScan()));
  }

  protected RowSet simpleExpected(int startIndex) {
    int offset = startIndex * 20;
    return fixture.rowSetBuilder(SCHEMA)
      .addRow(offset + 10, "fred")
      .addRow(offset + 20, "wilma")
      .build();
  }

  protected void verifyEmptyReader(ScanLifecycle scan) {
    RowBatchReader reader = scan.nextReader();
    assertTrue(reader.open());
    assertFalse(reader.next());
    assertFalse(scan.hasOutputSchema());
    reader.close();
  }

  protected void verifyStandardReader(ScanLifecycle scan, int offset) {
    RowBatchReader reader = scan.nextReader();
    reader = scan.nextReader();
    assertTrue(reader.open());
    assertTrue(reader.next());
    RowSetUtilities.verify(simpleExpected(0), fixture.wrap(reader.output()));
    assertFalse(reader.next());
    reader.close();
  }
}
