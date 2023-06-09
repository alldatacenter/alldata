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
package org.apache.drill.exec.physical.impl.validate;

import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet.SingleRowSet;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.RepeatedVarCharVector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(RowSetTests.class)
public class TestBatchValidator extends SubOperatorTest {

  public static class CapturingReporter implements BatchValidator.ErrorReporter {

    public List<String> errors = new ArrayList<>();

    @Override
    public void error(String name, ValueVector vector, String msg) {
      error(String.format("%s (%s): %s",
          name, vector.getClass().getSimpleName(), msg));
    }

    @Override
    public void warn(String name, ValueVector vector, String msg) {
      error(name, vector, msg);
    }

    @Override
    public void error(String msg) {
      errors.add(msg);
    }

    @Override
    public int errorCount() {
      return errors.size();
    }
  }

  @Test
  public void testValidFixed() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT)
        .addNullable("b", MinorType.INT)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow(10, 100)
        .addRow(20, 120)
        .addRow(30, null)
        .addRow(40, 140)
        .build();

    batch.clear();
  }

  @Test
  public void testValidVariable() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .addNullable("b", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow("col1.1", "col1.2")
        .addRow("col2.1", "col2.2")
        .addRow("col3.1", null)
        .addRow("col4.1", "col4.2")
        .build();

    batch.clear();
  }

  @Test
  public void testValidRepeated() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.INT, DataMode.REPEATED)
        .add("b", MinorType.VARCHAR, DataMode.REPEATED)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow(intArray(), strArray())
        .addRow(intArray(1, 2, 3), strArray("fred", "barney", "wilma"))
        .addRow(intArray(4), strArray("dino"))
        .build();

    assertTrue(BatchValidator.validate(batch.vectorAccessible()));
    batch.clear();
  }

  @Test
  public void testVariableMissingLast() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow("x")
        .addRow("y")
        .addRow("z")
        .build();

    // Here we are evil: stomp on the last offset to simulate corruption.
    // Don't do this in real code!

    VectorAccessible va = batch.vectorAccessible();
    ValueVector v = va.iterator().next().getValueVector();
    VarCharVector vc = (VarCharVector) v;
    UInt4Vector ov = vc.getOffsetVector();
    assertTrue(ov.getAccessor().get(3) > 0);
    ov.getMutator().set(3, 0);

    // Validator should catch the error.

    checkForError(batch, BAD_OFFSETS);
    batch.clear();
  }

  private static void checkForError(SingleRowSet batch, String expectedError) {
    CapturingReporter cr = new CapturingReporter();
    new BatchValidator(cr).validateBatch(batch.vectorAccessible(), batch.rowCount());
    assertTrue(cr.errors.size() > 0);
    Pattern p = Pattern.compile(expectedError);
    Matcher m = p.matcher(cr.errors.get(0));
    assertTrue(m.find());
  }

  @Test
  public void testVariableCorruptFirst() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow("x")
        .addRow("y")
        .addRow("z")
        .build();

    zapOffset(batch, 0, 1);

    // Validator should catch the error.

    checkForError(batch, "Offset \\(0\\) must be 0");
    batch.clear();
  }

  public void zapOffset(SingleRowSet batch, int index, int bogusValue) {

    // Here we are evil: stomp on an offset to simulate corruption.
    // Don't do this in real code!

    VectorAccessible va = batch.vectorAccessible();
    ValueVector v = va.iterator().next().getValueVector();
    VarCharVector vc = (VarCharVector) v;
    UInt4Vector ov = vc.getOffsetVector();
    ov.getMutator().set(index, bogusValue);
  }

  @Test
  public void testVariableCorruptMiddleLow() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow("xx")
        .addRow("yy")
        .addRow("zz")
        .build();

    zapOffset(batch, 2, 1);

    // Validator should catch the error.

    checkForError(batch, BAD_OFFSETS);
    batch.clear();
  }

  @Test
  public void testVariableCorruptMiddleHigh() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow("xx")
        .addRow("yy")
        .addRow("zz")
        .build();

    zapOffset(batch, 1, 10);

    // Validator should catch the error.

    checkForError(batch, "Invalid offset");
    batch.clear();
  }

  @Test
  public void testVariableCorruptLastOutOfRange() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow("xx")
        .addRow("yy")
        .addRow("zz")
        .build();

    zapOffset(batch, 3, 100_000);

    // Validator should catch the error.

    checkForError(batch, "Invalid offset");
    batch.clear();
  }

  private static final String BAD_OFFSETS = "Offset vector .* contained \\d+, expected >= \\d+";

  @Test
  public void testRepeatedBadArrayOffset() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR, DataMode.REPEATED)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow((Object) strArray())
        .addRow((Object) strArray("fred", "barney", "wilma"))
        .addRow((Object) strArray("dino"))
        .build();

    VectorAccessible va = batch.vectorAccessible();
    ValueVector v = va.iterator().next().getValueVector();
    RepeatedVarCharVector vc = (RepeatedVarCharVector) v;
    UInt4Vector ov = vc.getOffsetVector();
    ov.getMutator().set(3, 1);

    checkForError(batch, BAD_OFFSETS);
    batch.clear();
  }

  @Test
  public void testRepeatedBadValueOffset() {
    TupleMetadata schema = new SchemaBuilder()
        .add("a", MinorType.VARCHAR, DataMode.REPEATED)
        .buildSchema();

    SingleRowSet batch = fixture.rowSetBuilder(schema)
        .addRow((Object) strArray())
        .addRow((Object) strArray("fred", "barney", "wilma"))
        .addRow((Object) strArray("dino"))
        .build();

    VectorAccessible va = batch.vectorAccessible();
    ValueVector v = va.iterator().next().getValueVector();
    RepeatedVarCharVector rvc = (RepeatedVarCharVector) v;
    VarCharVector vc = rvc.getDataVector();
    UInt4Vector ov = vc.getOffsetVector();
    ov.getMutator().set(4, 100_000);

    checkForError(batch, "Invalid offset");
    batch.clear();
  }
}
