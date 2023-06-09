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
package org.apache.drill.exec.fn.hive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.Float4Vector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestHiveUDFs extends BaseTestQuery {

  @Test
  public void testGenericUDF() throws Throwable {
    String planString = Resources.toString(Resources.getResource("functions/hive/GenericUDF.json"), Charsets.UTF_8);
    List<QueryDataBatch> results = testPhysicalWithResults(planString);

    RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());
    for (QueryDataBatch result : results) {
      batchLoader.load(result.getHeader().getDef(), result.getData());
      if (batchLoader.getRecordCount() <= 0) {
        result.release();
        batchLoader.clear();
        continue;
      }
      // Output columns and types
      //  1. str1 : VarChar
      //  2. upperStr1 : NullableVarChar
      //  3. concat : NullableVarChar
      //  4. flt1 : Float4
      //  5. format_number : NullableFloat8
      //  6. nullableStr1 : NullableVarChar
      //  7. upperNullableStr1 : NullableVarChar
      VarCharVector str1V = (VarCharVector) batchLoader.getValueAccessorById(VarCharVector.class, 0).getValueVector();
      NullableVarCharVector upperStr1V = (NullableVarCharVector) batchLoader.getValueAccessorById(NullableVarCharVector.class, 1).getValueVector();
      NullableVarCharVector concatV = (NullableVarCharVector) batchLoader.getValueAccessorById(NullableVarCharVector.class, 2).getValueVector();
      Float4Vector flt1V = (Float4Vector) batchLoader.getValueAccessorById(Float4Vector.class, 3).getValueVector();
      NullableVarCharVector format_numberV = (NullableVarCharVector) batchLoader.getValueAccessorById(NullableVarCharVector.class, 4).getValueVector();
      NullableVarCharVector nullableStr1V = (NullableVarCharVector) batchLoader.getValueAccessorById(NullableVarCharVector.class, 5).getValueVector();
      NullableVarCharVector upperNullableStr1V = (NullableVarCharVector) batchLoader.getValueAccessorById(NullableVarCharVector.class, 6).getValueVector();

      for (int i=0; i<batchLoader.getRecordCount(); i++) {
        String in = new String(str1V.getAccessor().get(i), Charsets.UTF_8);
        String upper = new String(upperStr1V.getAccessor().get(i), Charsets.UTF_8);
        assertTrue(in.toUpperCase().equals(upper));


        String concat = new String(concatV.getAccessor().get(i), Charsets.UTF_8);
        assertTrue(concat.equals(in+"-"+in));

        String nullableStr1 = null;
        if (!nullableStr1V.getAccessor().isNull(i)) {
          nullableStr1 = new String(nullableStr1V.getAccessor().get(i), Charsets.UTF_8);
        }

        String upperNullableStr1 = null;
        if (!upperNullableStr1V.getAccessor().isNull(i)) {
          upperNullableStr1 = new String(upperNullableStr1V.getAccessor().get(i), Charsets.UTF_8);
        }

        assertEquals(nullableStr1 != null, upperNullableStr1 != null);
        if (nullableStr1 != null) {
          assertEquals(nullableStr1.toUpperCase(), upperNullableStr1);
        }
      }

      result.release();
      batchLoader.clear();
    }
  }

  @Test
  public void testUDF() throws Throwable {
    String planString = Resources.toString(Resources.getResource("functions/hive/UDF.json"), Charsets.UTF_8);
    List<QueryDataBatch> results = testPhysicalWithResults(planString);

    RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());
    for (QueryDataBatch result : results) {
      batchLoader.load(result.getHeader().getDef(), result.getData());
      if (batchLoader.getRecordCount() <= 0) {
        result.release();
        batchLoader.clear();
        continue;
      }

      // Output columns and types
      // 1. str1 : VarChar
      // 2. str1Length : Int
      // 3. str1Ascii : Int
      // 4. flt1 : Float4
      // 5. pow : Float8
      VarCharVector str1V = (VarCharVector) batchLoader.getValueAccessorById(VarCharVector.class, 0).getValueVector();
      BigIntVector str1LengthV = (BigIntVector) batchLoader.getValueAccessorById(BigIntVector.class, 1).getValueVector();
      IntVector str1AsciiV = (IntVector) batchLoader.getValueAccessorById(IntVector.class, 2).getValueVector();
      Float4Vector flt1V = (Float4Vector) batchLoader.getValueAccessorById(Float4Vector.class, 3).getValueVector();
      NullableFloat8Vector powV = (NullableFloat8Vector) batchLoader.getValueAccessorById(NullableFloat8Vector.class, 4).getValueVector();

      for (int i=0; i<batchLoader.getRecordCount(); i++) {
        String str1 = new String(str1V.getAccessor().get(i), Charsets.UTF_8);
        long str1Length = str1LengthV.getAccessor().get(i);
        assertTrue(str1.length() == str1Length);

        float flt1 = flt1V.getAccessor().get(i);

        double pow = 0;
        if (!powV.getAccessor().isNull(i)) {
          pow = powV.getAccessor().get(i);
          assertTrue(Math.pow(flt1, 2.0) == pow);
        }
      }

      result.release();
      batchLoader.clear();
    }
  }
}
