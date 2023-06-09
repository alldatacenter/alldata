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

import java.util.List;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.experimental.categories.Category;

/* This class tests the existing date types. Simply using date types
 * by casting from VarChar, performing basic functions and converting
 * back to VarChar.
 */
@Category({SlowTest.class})
public class TestExtractFunctions extends PopUnitTestBase {
  @Test
  public void testFromDate() throws Exception {
    long expectedValues[][] = { {00, 00, 02, 01, 1970}, {00, 00, 28, 12, 2008}, {00, 00, 27, 02, 2000} };
    testFrom("date", "/test_simple_date.json", "stringdate", expectedValues);
  }

  @Test
  @Ignore // failing due to some issue in castTime(varchar)
  public void testFromTime() throws Exception {
    long expectedValues[][] = { {20, 10, 00, 00, 0000}, {34, 11, 00, 00, 0000}, {24, 14, 00, 00, 0000} };
    testFrom("time", "/test_simple_time.json", "stringtime", expectedValues);
  }

  @Test
  public void testFromTimeStamp() throws Exception {
    long expectedValues[][] = { {20, 10, 02, 01, 1970}, {34, 11, 28, 12, 2008}, {24, 14, 27, 02, 2000} };
    testFrom("timestamp", "/test_simple_date.json", "stringdate", expectedValues);
  }

  @Test
  public void testFromInterval() throws Exception {
    long expectedValues[][] = {
      { 20, 01, 01, 02, 02},
      { 00, 00, 00, 02, 02},
      { 20, 01, 00, 00, 00},
      { 20, 01, 01, 02, 02},
      { 00, 00, 00, 00, 00},
      { -39, 00, 01, 10, 01}
    };
    testFrom("interval", "/test_simple_interval.json", "stringinterval", expectedValues);
  }

  @Test
  public void testFromIntervalDay() throws Exception {
    long expectedValues[][] = {
      {20, 01, 01, 00, 00},
      {00, 00, 00, 00, 00},
      {20, 01, 00, 00, 00},
      {20, 01, 01, 00, 00},
      {00, 00, 00, 00, 00},
      {-39, 00, 01, 00, 00}
    };
    testFrom("intervalday", "/test_simple_interval.json", "stringinterval", expectedValues);
  }

  @Test
  public void testFromIntervalYear() throws Exception {
    long expectedValues[][] = {
      {00, 00, 00, 02, 02},
      {00, 00, 00, 02, 02},
      {00, 00, 00, 00, 00},
      {00, 00, 00, 02, 02},
      {00, 00, 00, 00, 00},
      {00, 00, 00, 10, 01}
    };
    testFrom("intervalyear", "/test_simple_interval.json", "stringinterval", expectedValues);
  }

  private void testFrom(String fromType, String testDataFile, String columnName,
      long expectedValues[][]) throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
        Files.asCharSource(DrillFileUtils.getResourceAsFile("/functions/extractFrom.json"), Charsets.UTF_8)
            .read()
            .replace("#{TEST_TYPE}", fromType)
            .replace("#{TEST_FILE}", testDataFile)
            .replace("#{COLUMN_NAME}", columnName));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      for(int i=0; i<expectedValues.length; i++) {
        for(int j=0; j<expectedValues[i].length; j++) {
          NullableBigIntVector vv =
              (NullableBigIntVector) batchLoader.getValueAccessorById(NullableBigIntVector.class, j).getValueVector();
          assertEquals(expectedValues[i][j], vv.getAccessor().get(i));
        }
      }

      for(QueryDataBatch b : results){
        b.release();
      }
      batchLoader.clear();
    }
  }
}
