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
package org.apache.drill.hbase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.HbaseStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, HbaseStorageTest.class})
public class TestOrderedBytesConvertFunctions extends BaseTestQuery {

  private static final String CONVERSION_TEST_PHYSICAL_PLAN = "functions/conv/conversionTestWithPhysicalPlan.json";
  private static final float DELTA = (float) 0.0001;

  String textFileContent;

  @Test
  public void testOrderedBytesDouble() throws Throwable {
    verifyPhysicalPlan("convert_to(4.9e-324, 'DOUBLE_OB')", new byte[] {0x31, (byte)0x80, 0, 0, 0, 0, 0, 0, 0x01});
  }

  @Test
  public void testOrderedBytesDoubleConvertFrom() throws Throwable {
    verifyPhysicalPlan("convert_from(binary_string('\\x31\\x80\\x00\\x00\\x00\\x00\\x00\\x00\\x01'), 'DOUBLE_OB')", Double.valueOf(4.9e-324));
  }

  protected <T> void verifyPhysicalPlan(String expression, T expectedResults) throws Throwable {
    expression = expression.replace("\\", "\\\\\\\\"); // "\\\\\\\\" => Java => "\\\\" => JsonParser => "\\" => AntlrParser "\"

    if (textFileContent == null) {
      textFileContent = Resources.toString(Resources.getResource(CONVERSION_TEST_PHYSICAL_PLAN), Charsets.UTF_8);
    }
    String planString = textFileContent.replace("__CONVERT_EXPRESSION__", expression);

    verifyResults(expression, expectedResults, getRunResult(QueryType.PHYSICAL, planString));
  }

  protected Object[] getRunResult(QueryType queryType, String planString) throws Exception {
    List<QueryDataBatch> resultList = testRunAndReturn(queryType, planString);

    List<Object> res = new ArrayList<Object>();
    RecordBatchLoader loader = new RecordBatchLoader(getAllocator());
    for(QueryDataBatch result : resultList) {
      if (result.getData() != null) {
        loader.load(result.getHeader().getDef(), result.getData());
        ValueVector v = loader.iterator().next().getValueVector();
        for (int j = 0; j < v.getAccessor().getValueCount(); j++) {
          if  (v instanceof VarCharVector) {
            res.add(new String(((VarCharVector) v).getAccessor().get(j)));
          } else {
            res.add(v.getAccessor().getObject(j));
          }
        }
        loader.clear();
        result.release();
      }
    }

    return res.toArray();
  }

  protected <T> void verifyResults(String expression, T expectedResults, Object[] actualResults) throws Throwable {
    String testName = String.format("Expression: %s.", expression);
    assertEquals(testName, 1, actualResults.length);
    assertNotNull(testName, actualResults[0]);
    if (expectedResults.getClass().isArray()) {
      assertArraysEquals(testName, expectedResults, actualResults[0]);
    } else {
      assertEquals(testName, expectedResults, actualResults[0]);
    }
  }

  protected void assertArraysEquals(Object expected, Object actual) {
    assertArraysEquals(null, expected, actual);
  }

  protected void assertArraysEquals(String message, Object expected, Object actual) {
    if (expected instanceof byte[] && actual instanceof byte[]) {
      assertArrayEquals(message, (byte[]) expected, (byte[]) actual);
    } else if (expected instanceof Object[] && actual instanceof Object[]) {
      assertArrayEquals(message, (Object[]) expected, (Object[]) actual);
    } else if (expected instanceof char[] && actual instanceof char[]) {
      assertArrayEquals(message, (char[]) expected, (char[]) actual);
    } else if (expected instanceof short[] && actual instanceof short[]) {
      assertArrayEquals(message, (short[]) expected, (short[]) actual);
    } else if (expected instanceof int[] && actual instanceof int[]) {
      assertArrayEquals(message, (int[]) expected, (int[]) actual);
    } else if (expected instanceof long[] && actual instanceof long[]) {
      assertArrayEquals(message, (long[]) expected, (long[]) actual);
    } else if (expected instanceof float[] && actual instanceof float[]) {
      assertArrayEquals(message, (float[]) expected, (float[]) actual, DELTA);
    } else if (expected instanceof double[] && actual instanceof double[]) {
      assertArrayEquals(message, (double[]) expected, (double[]) actual, DELTA);
    } else {
      fail(String.format("%s: Error comparing arrays of type '%s' and '%s'",
          expected.getClass().getName(), (actual == null ? "null" : actual.getClass().getName())));
    }
  }
}
