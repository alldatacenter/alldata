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
package org.apache.drill.test.rowSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.shaded.guava.com.google.common.base.Optional;
import org.apache.drill.shaded.guava.com.google.common.collect.HashMultiset;
import org.apache.drill.shaded.guava.com.google.common.collect.Multiset;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.VariantReader;
import org.bouncycastle.util.Arrays;
import org.junit.Assert;

/**
 * For testing, compare the contents of two row sets (record batches)
 * to verify that they are identical. Supports masks to exclude certain
 * columns from comparison.
 * <p>
 * Drill rows are analogous to JSON documents: they can have scalars,
 * arrays and maps, with maps and lists holding maps, arrays and scalars.
 * This class walks the row structure tree to compare each structure
 * of two row sets checking counts, types and values to ensure that the
 * "actual" result set (result of a test) matches the "expected" result
 * set.
 * <p>
 * This class acts as an example of how to use the suite of reader
 * abstractions.
 */
public class RowSetComparison {

  /**
   * Row set with the expected outcome of a test. This is the "golden"
   * copy defined in the test itself.
   */
  private final RowSet expected;

  /**
   * Some tests wish to ignore certain (top-level) columns. If a
   * mask is provided, then only those columns with a <tt>true</tt>
   * will be verified.
   */
  private final boolean[] mask;

  /**
   * Floats and doubles do not compare exactly. This MathContext is used
   * to construct BigDecimals of the desired precision.
   */
  private MathContext scale = new MathContext(3);

  /**
  * Floats and doubles do not compare exactly. This delta is used
  * by JUnit for such comparisons. This is not a general solution;
  * it assumes that tests won't create values that require more than
  * three digits of precision.
  */
  private final double delta = 0.001;

  /**
   * Tests can skip the first n rows.
   */
  private int offset;
  private int span = -1;

  public RowSetComparison(RowSet expected) {
    this.expected = expected;

    // TODO: The mask only works at the top level presently

    mask = new boolean[expected.schema().size()];
    java.util.Arrays.fill(mask, true);
  }

  /**
   * Mark a specific column as excluded from comparisons.
   * @param colNo the index of the column to exclude
   * @return this builder
   */

  public RowSetComparison exclude(int colNo) {
    mask[colNo] = false;
    return this;
  }

  /**
   * Specifies a "selection" mask that determines which columns
   * to compare. Columns marked as "false" are omitted from the
   * comparison.
   *
   * @param flags variable-length list of column flags
   * @return this builder
   */
  public RowSetComparison withMask(Boolean...flags) {
    for (int i = 0; i < flags.length; i++) {
      mask[i] = flags[i];
    }
    return this;
  }

  /**
   * Specify the precision to use when comparing float or
   * double values.
   *
   * @param scale the precision to use for comparing floats and doubles. See {@link BigDecimal#scale()} for
   *              a definition scale.
   * @return this builder
   */
  public RowSetComparison withScale(int scale) {
    this.scale = new MathContext(scale);
    return this;
  }

  /**
   * Specify an offset into the row sets to start the comparison.
   * Usually combined with {@link #span(int)}.
   *
   * @param offset offset into the row set to start the comparison
   * @return this builder
   */
  public RowSetComparison offset(int offset) {
    this.offset = offset;
    return this;
  }

  /**
   * Specify a subset of rows to compare. Usually combined
   * with {@link #offset(int)}.
   *
   * @param span the number of rows to compare
   * @return this builder
   */
  public RowSetComparison span(int span) {
    this.span = span;
    return this;
  }

  private void compareSchemasAndCounts(RowSet actual) {
    if (!expected.schema().isEquivalent(actual.schema())) {
      // Avoid building the error string on every comparison,
      // only build on failures.
      fail("Schemas don't match.\n" +
        "Expected: " + expected.schema().toString() +
        "\nActual:   " + actual.schema().toString());
    }
    int testLength = getTestLength();
    int dataLength = offset + testLength;
    assertTrue("Missing expected rows", expected.rowCount() >= dataLength);
    assertTrue("Missing actual rows", actual.rowCount() >= dataLength);
  }

  private int getTestLength() {
    return span > -1 ? span : expected.rowCount() - offset;
  }

  public void unorderedVerify(RowSet actual) {
    compareSchemasAndCounts(actual);

    int testLength = getTestLength();
    RowSetReader er = expected.reader();
    RowSetReader ar = actual.reader();

    for (int i = 0; i < offset; i++) {
      er.next();
      ar.next();
    }

    final Multiset<List<Object>> expectedSet = HashMultiset.create();
    final Multiset<List<Object>> actualSet = HashMultiset.create();

    for (int rowCounter = 0; rowCounter < testLength; rowCounter++) {
      er.next();
      ar.next();

      expectedSet.add(buildRow(er));
      actualSet.add(buildRow(ar));
    }

    Assert.assertEquals(expectedSet, actualSet);
  }

  /**
   * Verifies the actual results, then frees memory
   * for both the expected and actual result sets.
   * @param actual the actual results to verify
   */
  public void unorderedVerifyAndClearAll(RowSet actual) {
    try {
      unorderedVerify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  private List<Object> buildRow(RowSetReader reader) {
    final List<Object> row = new ArrayList<>();

    for (int i = 0; i < mask.length; i++) {
      if (!mask[i]) {
        continue;
      }

      final ScalarReader scalarReader = reader.column(i).scalar();
      final Object value = getScalar(scalarReader);
      row.add(value);
    }

    return row;
  }

  /**
   * Verify the actual rows using the rules defined in this builder
   * @param actual the actual results to verify
   */
  public void verify(RowSet actual) {
    compareSchemasAndCounts(actual);
    int testLength = getTestLength();

    RowSetReader er = expected.reader();
    RowSetReader ar = actual.reader();

    for (int i = 0; i < offset; i++) {
      er.next();
      ar.next();
    }

    for (int i = 0; i < testLength; i++) {
      er.next();
      ar.next();
      String label = Integer.toString(er.logicalIndex() + 1);
      verifyRow(label, er, ar);
    }
  }

  /**
   * Convenience method to verify the actual results, then free memory
   * for the actual result sets.
   * @param actual the actual results to verify
   */
  public void verifyAndClear(RowSet actual) {
    try {
      verify(actual);
    } finally {
      actual.clear();
    }
  }

  /**
   * Convenience method to verify the actual results, then free memory
   * for both the expected and actual result sets.
   * @param actual the actual results to verify
   */
  public void verifyAndClearAll(RowSet actual) {
    try {
      verify(actual);
    } finally {
      expected.clear();
      actual.clear();
    }
  }

  private void verifyRow(String label, TupleReader er, TupleReader ar) {
    String prefix = label + ":";
    for (int i = 0; i < mask.length; i++) {
      if (! mask[i]) {
        continue;
      }
      verifyColumn(prefix + i, er.column(i), ar.column(i));
    }
  }

  private void verifyColumn(String label, ObjectReader ec, ObjectReader ac) {
    assertEquals(label, ec.type(), ac.type());
    switch (ec.type()) {
    case ARRAY:
      verifyArray(label, ec.array(), ac.array());
      break;
    case SCALAR:
      verifyScalar(label, ec.scalar(), ac.scalar());
      break;
    case TUPLE:
      verifyTuple(label, ec.tuple(), ac.tuple());
      break;
    case VARIANT:
      verifyVariant(label, ec.variant(), ac.variant());
      break;
    default:
      throw new IllegalStateException( "Unexpected type: " + ec.type());
    }
  }

  private void verifyTuple(String label, TupleReader er, TupleReader ar) {
    assertEquals(label + " - tuple count", er.columnCount(), ar.columnCount());
    String prefix = label + ":";
    for (int i = 0; i < er.columnCount(); i++) {
      verifyColumn(prefix + i, er.column(i), ar.column(i));
    }
  }

  private void verifyScalar(String label, ScalarReader ec, ScalarReader ac) {
    assertEquals(label + " - value type", ec.valueType(), ac.valueType());
    if (ec.isNull()) {
      assertTrue(label + " - column not null", ac.isNull());
      return;
    }
    if (!ec.isNull()) {
      assertFalse(label + " - column is null", ac.isNull());
    }

    switch (ec.valueType()) {
      case BYTES:
        byte[] expected = ac.getBytes();
        byte[] actual = ac.getBytes();
        assertEquals(label + " - byte lengths differ", expected.length, actual.length);
        assertTrue(label, Arrays.areEqual(expected, actual));
        break;

      // Double must be handled specially since BigDecimal cannot handle
      // INF or NAN double values.
      case FLOAT:
        assertEquals(label, ec.getFloat(), ac.getFloat(), delta);
        break;

      // Double must be handled specially since BigDecimal cannot handle
      // INF or NAN double values.
      case DOUBLE:
        assertEquals(label, ec.getDouble(), ac.getDouble(), delta);
        break;

      // repeated_contains is claimed to return a boolean,
      // actually returns a count, but in a bit field. To test
      // this function, we must treat BIT as an integer.
      default:
        assertEquals(label, getScalar(ec), getScalar(ac));
    }
  }

  private Object getScalar(final ScalarReader scalarReader) {
    if (scalarReader.isNull()) {
      return Optional.absent();
    }

    switch (scalarReader.valueType()) {
      case BYTES:
        return ByteBuffer.wrap(scalarReader.getBytes());
      case FLOAT:
        return new BigDecimal(scalarReader.getFloat(), this.scale).stripTrailingZeros();
      case DOUBLE:
        return new BigDecimal(scalarReader.getDouble(), this.scale).stripTrailingZeros();
      default:
        return scalarReader.getObject();
    }
  }

  private void verifyArray(String label, ArrayReader ea, ArrayReader aa) {
    assertEquals(label + " - array element type", ea.entryType(), aa.entryType());
    assertEquals(label + " - array length", ea.size(), aa.size());
    int i = 0;
    while (ea.next()) {
      assertTrue(aa.next());
      verifyColumn(label + "[" + i++ + "]", ea.entry(), aa.entry());
    }
  }

  private void verifyVariant(String label, VariantReader ev,
      VariantReader av) {
    assertEquals(label + " null", ev.isNull(), av.isNull());
    if (ev.isNull()) {
      return;
    }
    assertEquals(label + " type", ev.dataType(), av.dataType());
    switch (ev.dataType()) {
    case LIST:
      verifyArray(label, ev.array(), av.array());
      break;
    case MAP:
      verifyTuple(label, ev.tuple(), av.tuple());
      break;
    case UNION:
      throw new IllegalStateException("Unions not allowed in unions.");
    case GENERIC_OBJECT:
    case LATE:
    case NULL:
      throw new UnsupportedOperationException(ev.dataType().toString());
    default:
      verifyScalar(label, ev.scalar(), av.scalar());
    }
  }

  // TODO make a native RowSetComparison comparator
  public static class ObjectComparator implements Comparator<Object> {
    public static final ObjectComparator INSTANCE = new ObjectComparator();

    private ObjectComparator() {
    }

    @Override
    public int compare(Object a, Object b) {
      if (a instanceof Integer) {
        int aInt = (Integer) a;
        int bInt = (Integer) b;
        return aInt - bInt;
      } else if (a instanceof Long) {
        Long aLong = (Long) a;
        Long bLong = (Long) b;
        return aLong.compareTo(bLong);
      } else if (a instanceof Float) {
        Float aFloat = (Float) a;
        Float bFloat = (Float) b;
        return aFloat.compareTo(bFloat);
      } else if (a instanceof Double) {
        Double aDouble = (Double) a;
        Double bDouble = (Double) b;
        return aDouble.compareTo(bDouble);
      } else if (a instanceof String) {
        String aString = (String) a;
        String bString = (String) b;
        return aString.compareTo(bString);
      } else {
        throw new UnsupportedOperationException(String.format("Unsupported type %s", a.getClass().getCanonicalName()));
      }
    }
  }
}
