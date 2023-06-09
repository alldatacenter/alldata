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

import static org.junit.Assert.assertTrue;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.ScanOperatorExec;
import org.apache.drill.exec.physical.impl.scan.v3.FixedReceiver.Builder;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.Test;

public class TestFixedReceiver extends BaseScanTest {

  /**
   * Mock reader which writes columns as strings, using
   * standard conversions to convert to different types as
   * specified in a provided schema.
   */
  private static class MockReader implements ManagedReader {

    private final FixedReceiver receiver;

    public MockReader(SchemaNegotiator negotiator) {
      Builder builder = FixedReceiver.builderFor(negotiator)
          .schemaIsComplete();
      TupleMetadata readerSchema = new SchemaBuilder()
          .add("ti", MinorType.VARCHAR)
          .add("si", MinorType.VARCHAR)
          .add("int", MinorType.VARCHAR)
          .add("bi", MinorType.VARCHAR)
          .add("fl", MinorType.VARCHAR)
          .add("db", MinorType.VARCHAR)
          .buildSchema();
      receiver = builder.build(readerSchema);
    }

    @Override
    public boolean next() {
      if (receiver.rowWriter().loader().batchCount() > 0) {
        return false;
      }
      receiver.start();
      receiver.scalar(0).setString("11");
      receiver.scalar(1).setString("12");
      receiver.scalar(2).setString("13");
      receiver.scalar(3).setString("14");
      receiver.scalar(4).setString("15.5");
      receiver.scalar(5).setString("16.25");
      receiver.save();

      receiver.start();
      receiver.scalar("ti").setString("127");
      receiver.scalar("si").setString("32757");
      receiver.scalar("int").setString(Integer.toString(Integer.MAX_VALUE));
      receiver.scalar("bi").setString(Long.toString(Long.MAX_VALUE));
      receiver.scalar("fl").setString("10E6");
      receiver.scalar("db").setString("10E200");
      receiver.save();
      return true;
    }

    @Override
    public void close() { }
  }

  /**
   * Test the standard string-to-type conversion using an ad-hoc conversion
   * from the input type (the type used by the row set builder) to the output
   * (vector) type.
   */
  @Test
  public void testFixedReceiver() {

    // Create the provided and output schemas
    TupleMetadata outputSchema = new SchemaBuilder()
        .add("ti", MinorType.TINYINT)
        .add("si", MinorType.SMALLINT)
        .add("int", MinorType.INT)
        .add("bi", MinorType.BIGINT)
        .add("fl", MinorType.FLOAT4)
        .add("db", MinorType.FLOAT8)
        .buildSchema();

    // Create the scan
    BaseScanFixtureBuilder builder = simpleBuilder(negotiator -> new MockReader(negotiator));
    builder.builder.providedSchema(outputSchema);
    ScanFixture scanFixture = builder.build();
    ScanOperatorExec scan = scanFixture.scanOp;

    // Load test data using converters
    assertTrue(scan.next());

    // Build the expected vector without a type converter.
    final SingleRowSet expected = fixture.rowSetBuilder(outputSchema)
        .addRow(11, 12, 13, 14L, 15.5F, 16.25D)
        .addRow(127, 32757, Integer.MAX_VALUE, Long.MAX_VALUE, 10E6F, 10E200D)
        .build();

    // Compare
    VectorContainer container = scan.batchAccessor().container();
    RowSetUtilities.verify(expected, fixture.wrap(container));
    scanFixture.close();
  }
}
