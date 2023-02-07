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
package org.apache.drill.vector;

import static org.junit.Assert.assertEquals;

import org.apache.drill.categories.VectorTest;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.RepeatedVarCharVector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.test.SubOperatorTest;
import org.junit.Test;

import io.netty.buffer.DrillBuf;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(VectorTest.class)
public class TestFillEmpties extends SubOperatorTest {
  private static final Logger logger = LoggerFactory.getLogger(TestFillEmpties.class);

  @Test
  public void testNullableVarChar() {
    NullableVarCharVector vector = new NullableVarCharVector(SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.OPTIONAL), fixture.allocator());
    vector.allocateNew( );

    // Create "foo", null, "bar", but omit the null.

    NullableVarCharVector.Mutator mutator = vector.getMutator();
    byte[] value = makeValue( "foo" );
    mutator.setSafe(0, value, 0, value.length);

    value = makeValue("bar");
    mutator.setSafe(2, value, 0, value.length);

    visualize(vector, 3);
    verifyOffsets(vector.getValuesVector().getOffsetVector(), new int[] {0, 3, 3, 6});
    vector.close();
  }

  @Test
  public void testVarChar() {
    VarCharVector vector = new VarCharVector(SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REQUIRED), fixture.allocator());
    vector.allocateNew( );

    // Create "foo", null, "bar", but omit the null.

    VarCharVector.Mutator mutator = vector.getMutator();
    byte[] value = makeValue( "foo" );
    mutator.setSafe(0, value, 0, value.length);

    // Work around: test fails without this. But, only the new column writers
    // call this method.

    mutator.fillEmpties(0, 2);
    value = makeValue("bar");
    mutator.setSafe(2, value, 0, value.length);

    visualize(vector, 3);
    verifyOffsets(vector.getOffsetVector(), new int[] {0, 3, 3, 6});
    vector.close();
  }

  @Test
  public void testInt() {
    IntVector vector = new IntVector(SchemaBuilder.columnSchema("a", MinorType.INT, DataMode.REQUIRED), fixture.allocator());
    vector.allocateNew( );

    // Create 1, 0, 2, but omit the 0.

    IntVector.Mutator mutator = vector.getMutator();
    mutator.setSafe(0, 1);

    mutator.setSafe(2, 3);

    visualize(vector, 3);
    vector.close();
  }

  @Test
  public void testRepeatedVarChar() {
    RepeatedVarCharVector vector = new RepeatedVarCharVector(SchemaBuilder.columnSchema("a", MinorType.VARCHAR, DataMode.REPEATED), fixture.allocator());
    vector.allocateNew( );

    // Create "foo", null, "bar", but omit the null.

    RepeatedVarCharVector.Mutator mutator = vector.getMutator();
    mutator.startNewValue(0);
    byte[] value = makeValue( "a" );
    mutator.addSafe(0, value, 0, value.length);
    value = makeValue( "b" );
    mutator.addSafe(0, value, 0, value.length);

    // Work around: test fails without this. But, only the new column writers
    // call this method.

    mutator.fillEmpties(0, 2);
    mutator.startNewValue(2);
    value = makeValue( "c" );
    mutator.addSafe(2, value, 0, value.length);
    value = makeValue( "d" );
    mutator.addSafe(2, value, 0, value.length);

    visualize(vector, 3);
    verifyOffsets(vector.getOffsetVector(), new int[] {0, 2, 2, 4});
    verifyOffsets(vector.getDataVector().getOffsetVector(), new int[] {0, 1, 2, 3, 4});
    vector.close();
  }

  private void visualize(RepeatedVarCharVector vector, int valueCount) {
    visualize("Array Offsets", vector.getOffsetVector(), valueCount + 1);
    visualize(vector.getDataVector(), vector.getOffsetVector().getAccessor().get(valueCount));
  }

  private void visualize(IntVector vector, int valueCount) {
    final StringBuilder sb = new StringBuilder();
    sb.append("Values: [");
    IntVector.Accessor accessor = vector.getAccessor();
    for (int i = 0; i < valueCount; i++) {
      if (i > 0) { sb.append(" "); }
      sb.append(accessor.get(i));
    }
    sb.append("]");
    logger.info(sb.toString());
  }

  private void visualize(NullableVarCharVector vector, int valueCount) {
    visualize("Is-set", vector.getAccessor(), valueCount);
    visualize(vector.getValuesVector(), valueCount);
  }

  private void visualize(VarCharVector vector, int valueCount) {
    visualize("Offsets", vector.getOffsetVector(), valueCount + 1);
    visualize("Data", vector.getBuffer(), vector.getOffsetVector().getAccessor().get(valueCount));
  }

  private void visualize(String label, UInt4Vector offsetVector,
      int valueCount) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label + ": [");
    UInt4Vector.Accessor accessor = offsetVector.getAccessor();
    for (int i = 0; i < valueCount; i++) {
      if (i > 0) { sb.append(" "); }
      sb.append(accessor.get(i));
    }
    sb.append("]");
    logger.info(sb.toString());
  }

  private void visualize(String label, DrillBuf buffer, int valueCount) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label + ": [");
    for (int i = 0; i < valueCount; i++) {
      if (i > 0) { sb.append(" "); }
      sb.append((char) buffer.getByte(i));
    }
    sb.append("]");
    logger.info(sb.toString());
  }

  private void visualize(String label, BaseDataValueVector.BaseAccessor accessor, int valueCount) {
    final StringBuilder sb = new StringBuilder();
    sb.append(label + ": [");
    for (int i = 0; i < valueCount; i++) {
      if (i > 0) { sb.append(" "); }
      sb.append(accessor.isNull(i) ? 0 : 1);
    }
    sb.append("]");
    logger.info(sb.toString());
  }

  private void verifyOffsets(UInt4Vector offsetVector, int[] expected) {
    UInt4Vector.Accessor accessor = offsetVector.getAccessor();
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], accessor.get(i));
    }
  }

  /**
   * Create a test value. Works only for the ASCII subset of characters, obviously.
   * @param string
   * @return
   */
  private byte[] makeValue(String string) {
    byte value[] = new byte[string.length()];
    for (int i = 0; i < value.length; i++) {
      value[i] = (byte) string.charAt(i);
    }
    return value;
  }
}
