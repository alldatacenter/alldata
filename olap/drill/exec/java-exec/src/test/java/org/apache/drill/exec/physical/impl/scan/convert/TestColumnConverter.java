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
package org.apache.drill.exec.physical.impl.scan.convert;

import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetTestUtils;
import org.apache.drill.exec.physical.rowSet.RowSetWriter;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleNameSpace;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.test.SubOperatorTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests/demonstrates the generic form of the column/row format feature.
 * Creates a mock source as a proxy for some reader-specific row source.
 * Defines two column formats: one scalar, one array.
 * Runs a simple scan inventing values for the mock converters to convert.
 * Not really much to test, more a verification that the pattern works
 * in practice.
 */
@Category(RowSetTests.class)
public class TestColumnConverter extends SubOperatorTest {

  private static class MockSource {
    int rowNo;
  }

  private static abstract class MockConverter extends ColumnConverter {
    public final ObjectWriter objWriter;

    public MockConverter(ObjectWriter baseWriter) {
      super(scalarWriter(baseWriter));
      this.objWriter = baseWriter;
    }

    private static ScalarWriter scalarWriter(ObjectWriter baseWriter) {
      switch (baseWriter.type()) {
      case ARRAY:
        return baseWriter.array().scalar();
      case SCALAR:
        return baseWriter.scalar();
      default:
        throw new IllegalArgumentException(baseWriter.type().name());
      }
    }

    public abstract void setColumn(MockSource source);
    public ObjectWriter objWriter() { return objWriter; }
  }

  private static class MockIntConverter extends MockConverter {
    public MockIntConverter(ObjectWriter baseWriter) {
      super(baseWriter);
    }

    @Override
    public void setColumn(MockSource source) {
      // Mock value as string.
      String mockValue = Integer.toString(source.rowNo * 10);

      // Convert to int and save
      baseWriter.setInt(Integer.parseInt(mockValue));
    }
  }

  private static class MockArrayConverter extends MockConverter {
    public MockArrayConverter(ObjectWriter baseWriter) {
      super(baseWriter);
    }

    @Override
    public void setColumn(MockSource source) {
      // Mock value as array.
      int array[] = new int[3];
      for (int i = 0; i < 3; i++) {
        array[i] = source.rowNo * 100 + i + 1;
      }

      // Save as array
      ArrayWriter aw = objWriter().array();
      for (int i = 0; i < array.length; i++) {
        baseWriter.setInt(array[i]);
        aw.save();
      }
    }
  }

  private static RowSetWriter makeWriter(TupleMetadata outputSchema) {
    return RowSetTestUtils.makeWriter(fixture.allocator(), outputSchema);
  }

  /**
   * Test doing type conversion using (ad-hoc) properties on the
   * column metadata to drive conversion. Verifies that the properties
   * are available to the converter.
   */
  @Test
  public void testScalarConverter() {

    // Defined schema for scan output
    TupleMetadata outputSchema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addArray("b", MinorType.INT)
        .buildSchema();

    // Writer
    RowSetWriter writer = makeWriter(outputSchema);

    // Row and column formats
    TupleNameSpace<MockConverter> rowFormat = new TupleNameSpace<>();
    rowFormat.add(writer.column(0).schema().name(), new MockIntConverter(writer.column(0)));
    rowFormat.add(writer.column(1).schema().name(), new MockArrayConverter(writer.column(1)));

    // Write data as both a string as an integer
    MockSource source = new MockSource();
    for (int i = 0; i < 2; i++) {
      // Simulate a row
      source.rowNo = i + 1;
      for (int j = 0; j < rowFormat.count(); j++) {
        rowFormat.get(j).setColumn(source);
      }
      writer.save();
    }

    // Verify
    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(10, intArray(101, 102, 103))
        .addRow(20, intArray(201, 202, 203))
        .build();
    RowSetUtilities.verify(expected, writer.done());
  }
}
