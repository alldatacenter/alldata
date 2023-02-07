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
package org.apache.drill.exec.physical.impl.scan.v3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.v3.ManagedReader.EarlyEofException;
import org.apache.drill.exec.physical.impl.scan.v3.ScanFixture.ScanFixtureBuilder;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.OperatorFixture;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;

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
public class BaseScanTest extends SubOperatorTest {

  /**
   * Mock reader that pretends to have a schema at open time
   * like an HBase or JDBC reader.
   */
  protected static class MockEarlySchemaReader extends BaseMockBatchReader {

    public MockEarlySchemaReader(SchemaNegotiator schemaNegotiator) {
      TupleMetadata schema = new SchemaBuilder()
          .add("a", MinorType.INT)
          .addNullable("b", MinorType.VARCHAR, 10)
          .buildSchema();
      schemaNegotiator.tableSchema(schema, true);
      tableLoader = schemaNegotiator.build();
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

  /**
   * Fixture that creates the reader and gives a test access to
   * the reader.
   */
  public interface ReaderCreator {
    ManagedReader instance(SchemaNegotiator negotiator) throws EarlyEofException;
  }

  /**
   * Fixture that creates the reader and gives a test access to
   * the reader.
   */
  protected static abstract class ObservableCreator implements ReaderCreator {
    public ManagedReader reader;

    @Override
    public ManagedReader instance(SchemaNegotiator negotiator) throws EarlyEofException {
      reader = create(negotiator);
      return reader;
    }

    @SuppressWarnings("unchecked")
    public <T extends ManagedReader> T reader() { return (T) reader; }

    public abstract ManagedReader create(SchemaNegotiator negotiator) throws EarlyEofException;
  }

  /**
   * Mock reader that returns no schema and no records.
   */
  protected static class EofOnOpenReader implements ManagedReader {
    public EofOnOpenReader(SchemaNegotiator schemaNegotiator) throws EarlyEofException {
      throw new EarlyEofException();
    }

    @Override
    public boolean next() { return false; }

    @Override
    public void close() { }
  }

  public static class ReaderFactoryFixture implements ReaderFactory<SchemaNegotiator> {

    private final Iterator<ReaderCreator> iterator;

    public ReaderFactoryFixture(Iterator<ReaderCreator> iterator) {
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public ManagedReader next(SchemaNegotiator negotiator) throws EarlyEofException {
      return iterator.next().instance(negotiator);
    }
  }

  public static class BaseScanFixtureBuilder extends ScanFixtureBuilder {

    public ScanLifecycleBuilder builder = new ScanLifecycleBuilder();
    public final List<ReaderCreator> readers = new ArrayList<>();

    public BaseScanFixtureBuilder(OperatorFixture fixture) {
      super(fixture);
    }

    @Override
    public ScanLifecycleBuilder builder() { return builder; }

    public void addReader(ReaderCreator reader) {
      readers.add(reader);
    }

    public void addReaders(List<ReaderCreator> readers) {
      this.readers.addAll(readers);
    }

    public void addReaders(ReaderCreator...readers) {
      for (ReaderCreator reader : readers) {
        addReader(reader);
      }
    }

    @Override
    public ScanFixture build() {
      builder.readerFactory(new ReaderFactoryFixture(readers.iterator()));
      return super.build();
    }
  }

  @SafeVarargs
  public static BaseScanFixtureBuilder simpleBuilder(ReaderCreator...readers) {
    BaseScanFixtureBuilder builder = new BaseScanFixtureBuilder(fixture);
    builder.projectAll();
    builder.addReaders(readers);
    return builder;
  }

  @SafeVarargs
  public static ScanFixture simpleFixture(ReaderCreator...readers) {
    return simpleBuilder(readers).build();
  }
}
