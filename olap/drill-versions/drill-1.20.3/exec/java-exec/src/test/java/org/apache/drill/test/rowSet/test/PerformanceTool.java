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
package org.apache.drill.test.rowSet.test;

import java.util.concurrent.TimeUnit;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.RepeatedIntVector;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.IntColumnWriter;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.writer.AbstractArrayWriter.ArrayObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.NullableScalarWriter;
import org.apache.drill.exec.vector.accessor.writer.ScalarArrayWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.test.OperatorFixture;

/**
 * Tests the performance of the writers compared to using the value
 * vector mutators directly. In order to achieve apples-to-apples
 * comparison, the tests work directly with individual columns in
 * the writer case; the row writer level is omitted as the row writer
 * simulates the reader logic previously used to write to vectors.
 * <p>
 * Current results:
 * <ul>
 * <li>Required and nullable writers are slightly faster than the
 * corresponding vector mutator methods.</li>
 * <li>Writer is 230% faster than a repeated mutator.</li>
 * </ul>
 *
 * The key reason for the converged performance (now compared to earlier
 * results below) is that both paths now use bounds-checking optimizations.
 * <p>
 * Prior results before the common bounds-check optimizations:
 * <ul>
 * <li>Writer is 42% faster than a required mutator.</li>
 * <li>Writer is 73% faster than a nullable mutator.</li>
 * <li>Writer is 407% faster than a repeated mutator.</li>
 * </ul>
 * Since performance is critical for this component (this is the
 * ultimate "inner loop", please run these tests periodically to
 * ensure that performance does not drop; it is very easy to add
 * a bit of code here or there that greatly impacts performance.
 * <p>
 * This is not a JUnit test. Rather, it is a stand-alone program
 * which must be run explicitly. One handy way is to run it from
 * your IDE. If using Eclipse, monitor the system to wait for Eclipse
 * to finish its background processing before launching.
 */

public class PerformanceTool {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PerformanceTool.class);

  public static final int ROW_COUNT = 16 * 1024 * 1024 / 4;
  public static final int ITERATIONS = 300;

  public static abstract class PerfTester {
    final TupleMetadata rowSchema;
    final MaterializedField field;
    final OperatorFixture fixture;
    final String label;
    final Stopwatch timer = Stopwatch.createUnstarted();

    public PerfTester(OperatorFixture fixture, DataMode mode, String label) {
      this.fixture = fixture;
      this.label = label;
      field = SchemaBuilder.columnSchema("a", MinorType.INT, mode);
      rowSchema = new SchemaBuilder()
                  .add(field)
                  .buildSchema();
    }

    public void runTest() {
      for (int i = 0; i < ITERATIONS; i++) {
        doTest();
      }

      logger.info("{}: {}", label, timer.elapsed(TimeUnit.MILLISECONDS));
    }

    public abstract void doTest();
  }

  public static class RequiredVectorTester extends PerfTester {

    public RequiredVectorTester(OperatorFixture fixture) {
      super(fixture, DataMode.REQUIRED, "Required vector");
    }

    @Override
    public void doTest() {
      try (IntVector vector = new IntVector(field, fixture.allocator());) {
        vector.allocateNew(4096);
        IntVector.Mutator mutator = vector.getMutator();
        timer.start();
        for (int i = 0; i < ROW_COUNT; i++) {
          mutator.setSafe(i, 1234);
        }
        timer.stop();
      }
    }
  }

  public static class NullableVectorTester extends PerfTester {

    public NullableVectorTester(OperatorFixture fixture) {
      super(fixture, DataMode.OPTIONAL, "Nullable vector");
    }

    @Override
    public void doTest() {
      try (NullableIntVector vector = new NullableIntVector(field, fixture.allocator());) {
        vector.allocateNew(4096);
        NullableIntVector.Mutator mutator = vector.getMutator();
        timer.start();
        for (int i = 0; i < ROW_COUNT; i++) {
          mutator.setSafe(i, 1234);
        }
        timer.stop();
      }
    }
  }

  public static class RepeatedVectorTester extends PerfTester {

    public RepeatedVectorTester(OperatorFixture fixture) {
      super(fixture, DataMode.REQUIRED, "Repeated vector");
    }

    @Override
    public void doTest() {
      try (RepeatedIntVector vector = new RepeatedIntVector(field, fixture.allocator());) {
        vector.allocateNew(ROW_COUNT, 5 * ROW_COUNT);
        RepeatedIntVector.Mutator mutator = vector.getMutator();
        timer.start();
        for (int i = 0; i < ROW_COUNT / 5; i++) {
          mutator.startNewValue(i);
          mutator.addSafe(i, 12341);
          mutator.addSafe(i, 12342);
          mutator.addSafe(i, 12343);
          mutator.addSafe(i, 12344);
          mutator.addSafe(i, 12345);
        }
        timer.stop();
      }
    }
  }

  private static class TestWriterIndex implements ColumnWriterIndex {

    public int index;

    @Override
    public int vectorIndex() { return index; }

    @Override
    public final void nextElement() { index++; }

    @Override
    public final void prevElement() { }

    @Override
    public void rollover() { }

    @Override
    public int rowStartIndex() { return index; }

    @Override
    public ColumnWriterIndex outerIndex() { return null; }
  }

  public static class RequiredWriterTester extends PerfTester {

    public RequiredWriterTester(OperatorFixture fixture) {
      super(fixture, DataMode.REQUIRED, "Required writer");
    }

    @Override
    public void doTest() {
      try (IntVector vector = new IntVector(rowSchema.column(0), fixture.allocator());) {
        vector.allocateNew(ROW_COUNT);
        IntColumnWriter colWriter = new IntColumnWriter(vector);
        TestWriterIndex index = new TestWriterIndex();
        colWriter.bindIndex(index);
        colWriter.startWrite();
        timer.start();
        while (index.index < ROW_COUNT) {
          colWriter.setInt(1234);
        }
        timer.stop();
        colWriter.endWrite();
      }
    }
  }

  public static class NullableWriterTester extends PerfTester {

    public NullableWriterTester(OperatorFixture fixture) {
      super(fixture, DataMode.OPTIONAL, "Nullable writer");
    }

    @Override
    public void doTest() {
      try (NullableIntVector vector = new NullableIntVector(rowSchema.column(0), fixture.allocator());) {
        vector.allocateNew(ROW_COUNT);
        ColumnMetadata colSchema = MetadataUtils.fromField(vector.getField());
        NullableScalarWriter colWriter = new NullableScalarWriter(colSchema,
            vector, new IntColumnWriter(vector.getValuesVector()));
        TestWriterIndex index = new TestWriterIndex();
        colWriter.bindIndex(index);
        colWriter.startWrite();
        timer.start();
        while (index.index < ROW_COUNT) {
          colWriter.setInt(1234);
        }
        timer.stop();
        colWriter.endWrite();
      }
    }
  }

  public static class ArrayWriterTester extends PerfTester {

    public ArrayWriterTester(OperatorFixture fixture) {
      super(fixture, DataMode.REQUIRED, "Array writer");
    }

    @Override
    public void doTest() {
      try (RepeatedIntVector vector = new RepeatedIntVector(rowSchema.column(0), fixture.allocator());) {
        vector.allocateNew(ROW_COUNT, 5 * ROW_COUNT);
        IntColumnWriter colWriter = new IntColumnWriter(vector.getDataVector());
        ColumnMetadata colSchema = MetadataUtils.fromField(vector.getField());
        ArrayObjectWriter arrayWriter = ScalarArrayWriter.build(colSchema, vector, colWriter);
        TestWriterIndex index = new TestWriterIndex();
        arrayWriter.events().bindIndex(index);
        arrayWriter.events().startWrite();
        timer.start();
        for ( ; index.index < ROW_COUNT / 5; index.index++) {
          arrayWriter.events().startRow();
          colWriter.setInt(12341);
          colWriter.setInt(12342);
          colWriter.setInt(12343);
          colWriter.setInt(12344);
          colWriter.setInt(12345);
          arrayWriter.events().endArrayValue();
        }
        timer.stop();
        arrayWriter.events().endWrite();
      }
    }
  }

  public static void main(String args[]) {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(null);) {
      for (int i = 0; i < 2; i++) {
        logger.info((i==0) ? "Warmup" : "Test run");
        new RequiredVectorTester(fixture).runTest();
        new RequiredWriterTester(fixture).runTest();
        new NullableVectorTester(fixture).runTest();
        new NullableWriterTester(fixture).runTest();
        new RepeatedVectorTester(fixture).runTest();
        new ArrayWriterTester(fixture).runTest();
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      logger.error("Exception", e);
    }
  }
}
