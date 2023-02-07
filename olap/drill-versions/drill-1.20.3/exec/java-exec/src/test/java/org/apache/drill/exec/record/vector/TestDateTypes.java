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
package org.apache.drill.exec.record.vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.apache.drill.categories.SlowTest;
import org.apache.drill.categories.VectorTest;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/* Tests the existing date types. Simply using date types
 * by casting from VarChar, performing basic functions and converting
 * back to VarChar.
 */
@Category({SlowTest.class, VectorTest.class})
public class TestDateTypes extends PopUnitTestBase {

  @Test
  public void testDate() throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
       Drillbit bit = new Drillbit(CONFIG, serviceSet);
       DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/vector/test_date.json"), Charsets.UTF_8).read()
                      .replace("#{TEST_FILE}", "/test_simple_date.json"));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      for (VectorWrapper<?> v : batchLoader) {

        ValueVector.Accessor accessor = v.getValueVector().getAccessor();

        assertEquals((accessor.getObject(0).toString()), ("1970-01-02"));
        assertEquals((accessor.getObject(1).toString()), ("2008-12-28"));
        assertEquals((accessor.getObject(2).toString()), ("2000-02-27"));
      }

      batchLoader.clear();
      for(QueryDataBatch b : results){
        b.release();
      }
    }
  }

  @Test
  public void testSortDate() throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
       Drillbit bit = new Drillbit(CONFIG, serviceSet);
       DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/vector/test_sort_date.json"), Charsets.UTF_8).read()
                      .replace("#{TEST_FILE}", "/test_simple_date.json"));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(1);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      for (VectorWrapper<?> v : batchLoader) {

        ValueVector.Accessor accessor = v.getValueVector().getAccessor();

        assertEquals((accessor.getObject(0).toString()), "1970-01-02");
        assertEquals((accessor.getObject(1).toString()), "2000-02-27");
        assertEquals((accessor.getObject(2).toString()), "2008-12-28");
      }

      batchLoader.clear();
      for(QueryDataBatch b : results){
        b.release();
      }
    }
  }

  @Test
  public void testTimeStamp() throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
       Drillbit bit = new Drillbit(CONFIG, serviceSet);
       DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/vector/test_timestamp.json"), Charsets.UTF_8).read()
                      .replace("#{TEST_FILE}", "/test_simple_date.json"));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      for (VectorWrapper<?> v : batchLoader) {

        ValueVector.Accessor accessor = v.getValueVector().getAccessor();

        assertEquals(accessor.getObject(0).toString(),"1970-01-02 10:20:33.000");
        assertEquals(accessor.getObject(1).toString(),"2008-12-28 11:34:00.129");
        assertEquals(accessor.getObject(2).toString(), "2000-02-27 14:24:00.000");
      }

      batchLoader.clear();
      for(QueryDataBatch b : results){
        b.release();
      }
    }
  }

  @Test
  public void testInterval() throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/vector/test_interval.json"), Charsets.UTF_8).read()
                      .replace("#{TEST_FILE}", "/test_simple_interval.json"));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      Iterator<VectorWrapper<?>> itr = batchLoader.iterator();

      ValueVector.Accessor accessor = itr.next().getValueVector().getAccessor();

      // Check the interval type
      assertEquals((accessor.getObject(0).toString()), ("2 years 2 months 1 day 1:20:35.0"));
      assertEquals((accessor.getObject(1).toString()), ("2 years 2 months 0 days 0:0:0.0"));
      assertEquals((accessor.getObject(2).toString()), ("0 years 0 months 0 days 1:20:35.0"));
      assertEquals((accessor.getObject(3).toString()),("2 years 2 months 1 day 1:20:35.897"));
      assertEquals((accessor.getObject(4).toString()), ("0 years 0 months 0 days 0:0:35.4"));
      assertEquals((accessor.getObject(5).toString()), ("1 year 10 months 1 day 0:-39:-25.0"));

      accessor = itr.next().getValueVector().getAccessor();

      // Check the interval year type
      assertEquals((accessor.getObject(0).toString()), ("2 years 2 months "));
      assertEquals((accessor.getObject(1).toString()), ("2 years 2 months "));
      assertEquals((accessor.getObject(2).toString()), ("0 years 0 months "));
      assertEquals((accessor.getObject(3).toString()), ("2 years 2 months "));
      assertEquals((accessor.getObject(4).toString()), ("0 years 0 months "));
      assertEquals((accessor.getObject(5).toString()), ("1 year 10 months "));


      accessor = itr.next().getValueVector().getAccessor();

      // Check the interval day type
      assertEquals((accessor.getObject(0).toString()), ("1 day 1:20:35.0"));
      assertEquals((accessor.getObject(1).toString()), ("0 days 0:0:0.0"));
      assertEquals((accessor.getObject(2).toString()), ("0 days 1:20:35.0"));
      assertEquals((accessor.getObject(3).toString()), ("1 day 1:20:35.897"));
      assertEquals((accessor.getObject(4).toString()), ("0 days 0:0:35.4"));
      assertEquals((accessor.getObject(5).toString()), ("1 day 0:-39:-25.0"));

      batchLoader.clear();
      for(QueryDataBatch b : results){
        b.release();
      }
    }
  }

  @Test
  public void testLiterals() throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/vector/test_all_date_literals.json"), Charsets.UTF_8).read()
                      .replace("#{TEST_FILE}", "/test_simple_date.json"));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      String result[] = {"2008-02-27",
                         "2008-02-27 01:02:03.000",
                         "10:11:13.999",
                         "2 years 2 months 3 days 0:1:3.89"};

      int idx = 0;

      for (VectorWrapper<?> v : batchLoader) {

        ValueVector.Accessor accessor = v.getValueVector().getAccessor();

        assertEquals((accessor.getObject(0).toString()), (result[idx++]));
      }

      batchLoader.clear();
      for(QueryDataBatch b : results){
        b.release();
      }
    }
  }

  @Test
  public void testDateAdd() throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {

      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
              Files.asCharSource(DrillFileUtils.getResourceAsFile("/record/vector/test_date_add.json"), Charsets.UTF_8).read()
                      .replace("#{TEST_FILE}", "/test_simple_date.json"));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      for (VectorWrapper<?> v : batchLoader) {

        ValueVector.Accessor accessor = v.getValueVector().getAccessor();

        assertEquals((accessor.getObject(0).toString()), ("2008-03-27 00:00:00.000"));
      }

      batchLoader.clear();
      for(QueryDataBatch b : results){
        b.release();
      }
    }
  }
}
