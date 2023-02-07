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
package org.apache.drill.exec.physical.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.util.DecimalUtility;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.categories.SlowTest;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class})
public class TestDecimal extends PopUnitTestBase {

    @Test
    public void testSimpleDecimal() throws Exception {

        /* Function checks casting from VarChar to Decimal9, Decimal18 and vice versa
         * Also tests instances where the scale might have to truncated when scale provided < input fraction
         */
        try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
             Drillbit bit = new Drillbit(CONFIG, serviceSet);
             DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.asCharSource(DrillFileUtils.getResourceAsFile("/decimal/cast_simple_decimal.json"), Charsets.UTF_8)
                            .read().replace("#{TEST_FILE}", "/input_simple_decimal.json")
            );

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            String decimal9Output[] = {"99.0000", "11.1235", "0.1000", "-0.1200", "-123.1234", "-1.0001"};
            String decimal18Output[] = {"123456789.000000000", "11.123456789", "0.100000000", "-0.100400000", "-987654321.123456789", "-2.030100000"};

            Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

            // Check the output of decimal9
            ValueVector.Accessor dec9Accessor = itr.next().getValueVector().getAccessor();
            ValueVector.Accessor dec18Accessor = itr.next().getValueVector().getAccessor();


            for (int i = 0; i < dec9Accessor.getValueCount(); i++) {
                assertEquals(dec9Accessor.getObject(i).toString(), decimal9Output[i]);
                assertEquals(dec18Accessor.getObject(i).toString(), decimal18Output[i]);
            }
            assertEquals(6, dec9Accessor.getValueCount());
            assertEquals(6, dec18Accessor.getValueCount());

            batchLoader.clear();
            for (QueryDataBatch result : results) {
              result.release();
            }
        }
    }

    @Test
    public void testCastFromFloat() throws Exception {

        // Function checks for casting from Float, Double to Decimal data types
        try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
             Drillbit bit = new Drillbit(CONFIG, serviceSet);
             DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.asCharSource(DrillFileUtils.getResourceAsFile("/decimal/cast_float_decimal.json"), Charsets.UTF_8)
                            .read().replace("#{TEST_FILE}", "/input_simple_decimal.json")
            );

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            String decimal9Output[] = {"99.0000", "11.1235", "0.1000", "-0.1200", "-123.1234", "-1.0001"};
            String decimal38Output[] = {"123456789.0000", "11.1235", "0.1000", "-0.1004", "-987654321.1235", "-2.0301"};

            Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

            // Check the output of decimal9
            ValueVector.Accessor dec9Accessor = itr.next().getValueVector().getAccessor();
            ValueVector.Accessor dec38Accessor = itr.next().getValueVector().getAccessor();


            for (int i = 0; i < dec9Accessor.getValueCount(); i++) {
                assertEquals(dec9Accessor.getObject(i).toString(), decimal9Output[i]);
                assertEquals(dec38Accessor.getObject(i).toString(), decimal38Output[i]);
            }
            assertEquals(6, dec9Accessor.getValueCount());
            assertEquals(6, dec38Accessor.getValueCount());

            batchLoader.clear();
            for (QueryDataBatch result : results) {
              result.release();
            }
        }
    }

    @Test
    public void testSimpleDecimalArithmetic() throws Exception {

        // Function checks arithmetic operations on Decimal18
        try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
             Drillbit bit = new Drillbit(CONFIG, serviceSet);
             DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.asCharSource(DrillFileUtils.getResourceAsFile("/decimal/simple_decimal_arithmetic.json"), Charsets.UTF_8)
                            .read().replace("#{TEST_FILE}", "/input_simple_decimal.json")
            );

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            String addOutput[] = {"123456888.0", "22.2", "0.2", "-0.2", "-987654444.2","-3.0"};
            String subtractOutput[] = {"123456690.0", "0.0", "0.0", "0.0", "-987654198.0", "-1.0"};
            String multiplyOutput[] = {"12222222111.00", "123.21", "0.01", "0.01",  "121580246927.41", "2.00"};

            Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

            // Check the output of add
            ValueVector.Accessor addAccessor = itr.next().getValueVector().getAccessor();
            ValueVector.Accessor subAccessor = itr.next().getValueVector().getAccessor();
            ValueVector.Accessor mulAccessor = itr.next().getValueVector().getAccessor();

            for (int i = 0; i < addAccessor.getValueCount(); i++) {
                assertEquals(addAccessor.getObject(i).toString(), addOutput[i]);
                assertEquals(subAccessor.getObject(i).toString(), subtractOutput[i]);
                assertEquals(mulAccessor.getObject(i).toString(), multiplyOutput[i]);

            }
            assertEquals(6, addAccessor.getValueCount());
            assertEquals(6, subAccessor.getValueCount());
            assertEquals(6, mulAccessor.getValueCount());

            batchLoader.clear();
            for (QueryDataBatch result : results) {
              result.release();
            }
        }
    }

    @Test
    public void testComplexDecimal() throws Exception {

        /* Function checks casting between varchar and decimal38sparse
         * Also checks arithmetic on decimal38sparse
         */
        try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
             Drillbit bit = new Drillbit(CONFIG, serviceSet);
             DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.asCharSource(DrillFileUtils.getResourceAsFile("/decimal/test_decimal_complex.json"), Charsets.UTF_8)
                            .read().replace("#{TEST_FILE}", "/input_complex_decimal.json")
            );

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(0);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            String addOutput[] = {"-99999998877.700000000", "11.423456789", "123456789.100000000", "-0.119998000", "100000000112.423456789", "-99999999879.907000000", "123456789123456801.300000000"};
            String subtractOutput[] = {"-100000001124.300000000", "10.823456789", "-123456788.900000000", "-0.120002000", "99999999889.823456789", "-100000000122.093000000", "123456789123456776.700000000"};

            Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

            ValueVector.Accessor addAccessor = itr.next().getValueVector().getAccessor();
            ValueVector.Accessor subAccessor = itr.next().getValueVector().getAccessor();

            for (int i = 0; i < addAccessor.getValueCount(); i++) {
                assertEquals(addAccessor.getObject(i).toString(), addOutput[i]);
                assertEquals(subAccessor.getObject(i).toString(), subtractOutput[i]);
            }
            assertEquals(7, addAccessor.getValueCount());
            assertEquals(7, subAccessor.getValueCount());

            batchLoader.clear();
            for (QueryDataBatch result : results) {
              result.release();
            }
        }
    }

    @Test
    public void testComplexDecimalSort() throws Exception {

        // Function checks if sort output on complex decimal type works
        try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
             Drillbit bit = new Drillbit(CONFIG, serviceSet);
             DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

            // run query.
            bit.run();
            client.connect();
            List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
                    Files.asCharSource(DrillFileUtils.getResourceAsFile("/decimal/test_decimal_sort_complex.json"), Charsets.UTF_8)
                            .read().replace("#{TEST_FILE}", "/input_sort_complex_decimal.json")
            );

            RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

            QueryDataBatch batch = results.get(1);
            assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

            String sortOutput[] = {"-100000000001.000000000000",
                                   "-100000000001.000000000000",
                                   "-145456789.120123000000",
                                   "-0.120000000000",
                                   "0.100000000001",
                                   "11.123456789012",
                                   "1278789.100000000000",
                                   "145456789.120123000000",
                                   "100000000001.123456789001",
                                   "123456789123456789.000000000000"};

            Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

            // Check the output of sort
            VectorWrapper<?> v = itr.next();
            ValueVector.Accessor accessor = v.getValueVector().getAccessor();

            for (int i = 0; i < accessor.getValueCount(); i++) {
                assertEquals(sortOutput[i], accessor.getObject(i).toString());
            }
            assertEquals(10, accessor.getValueCount());

            batchLoader.clear();
            for (QueryDataBatch result : results) {
              result.release();
            }
        }
    }

  @Test
  public void testSimpleDecimalMathFunc() throws Exception {

    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
          Files.asCharSource(DrillFileUtils.getResourceAsFile("/decimal/simple_decimal_math.json"), Charsets.UTF_8)
              .read().replace("#{TEST_FILE}", "/input_simple_decimal.json")
      );

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

      // Check the output of decimal18
      ValueVector.Accessor dec18Accessor = itr.next().getValueVector().getAccessor();

      assertEquals(6, dec18Accessor.getValueCount());

      batchLoader.clear();
      for (QueryDataBatch result : results) {
        result.release();
      }
    }
  }

  @Test
  public void testGetMaxBytesSizeForPrecision() {
    for (int i = 0; i < 10_000; i++) {
      assertEquals(String.format("Bytes size does not match for precision %s", i),
          getMaxBytesSizeForPrecisionFromBigInteger(i),
          DecimalUtility.getMaxBytesSizeForPrecision(i));
    }
  }

  private static int getMaxBytesSizeForPrecisionFromBigInteger(int precision) {
    if (precision == 0) {
      return 0;
    }

    StringBuilder sb = new StringBuilder("-");
    for (int i = 0; i < precision; i++) {
      sb.append(9);
    }
    return new BigInteger(sb.toString()).toByteArray().length;
  }
}
