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
package org.apache.drill.exec.fn.impl;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.pop.PopUnitTestBase;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.RemoteServiceSet;
import org.apache.drill.exec.vector.ValueVector;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestDateFunctions extends PopUnitTestBase {

  public void testCommon(String[] expectedResults, String physicalPlan, String resourceFile) throws Exception {
    try (RemoteServiceSet serviceSet = RemoteServiceSet.getLocalServiceSet();
         Drillbit bit = new Drillbit(CONFIG, serviceSet);
         DrillClient client = new DrillClient(CONFIG, serviceSet.getCoordinator())) {
      // run query.
      bit.run();
      client.connect();
      List<QueryDataBatch> results = client.runQuery(org.apache.drill.exec.proto.UserBitShared.QueryType.PHYSICAL,
        Files.asCharSource(DrillFileUtils.getResourceAsFile(physicalPlan), Charsets.UTF_8).read()
          .replace("#{TEST_FILE}", resourceFile));

      RecordBatchLoader batchLoader = new RecordBatchLoader(bit.getContext().getAllocator());

      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));


      int i = 0;
      for (VectorWrapper<?> v : batchLoader) {
        ValueVector.Accessor accessor = v.getValueVector().getAccessor();
        assertEquals(expectedResults[i++], accessor.getObject(0).toString());
      }

      batchLoader.clear();
      for (QueryDataBatch b : results) {
        b.release();
      }
    }
  }

  @Test
  @Ignore("relies on particular timezone")
  public void testDateIntervalArithmetic() throws Exception {
    String expectedResults[] = {"2009-02-23T00:00:00.000-08:00",
                                "2008-02-24T00:00:00.000-08:00",
                                "1970-01-01T13:20:33.000-08:00",
                                "2008-02-24T12:00:00.000-08:00",
                                "2009-04-23T12:00:00.000-07:00",
                                "2008-02-24T12:00:00.000-08:00",
                                "2009-04-23T12:00:00.000-07:00",
                                "2009-02-23T00:00:00.000-08:00",
                                "2008-02-24T00:00:00.000-08:00",
                                "1970-01-01T13:20:33.000-08:00",
                                "2008-02-24T12:00:00.000-08:00",
                                "2009-04-23T12:00:00.000-07:00",
                                "2008-02-24T12:00:00.000-08:00",
                                "2009-04-23T12:00:00.000-07:00"};
    testCommon(expectedResults, "/functions/date/date_interval_arithmetic.json", "/test_simple_date.json");
  }

  @Test
  public void testDateDifferenceArithmetic() throws Exception {

    String[] expectedResults = {"P365D",
                                "P-366DT-60S",
                                "PT39600S"};
    testCommon(expectedResults, "/functions/date/date_difference_arithmetic.json", "/test_simple_date.json");
  }

  @Test
  public void testAge() throws Exception {
    String[] expectedResults = {
      "P107M30DT82800S",
      "P108MT3600S",
      "P170M12D",
      "P-170M-12D",
      "P-39M-2DT-63573S",
      "P-39M-3DT-22827S",
      "PT3540S",
      "PT0S",
      "PT0S",
      "P-3D",
      "P2DT84600S"
    };
    testCommon(expectedResults, "/functions/date/age.json", "/test_simple_date.json");
  }

  @Test
  public void testIntervalArithmetic() throws Exception {

    String expectedResults[] = {"P2Y2M",
                                "P2DT3723S",
                                "P2M",
                                "PT3723S",
                                "P28M",
                                "PT7206S",
                                "P7M",
                                "PT1801.500S",
                                "P33M18D",
                                "PT8647.200S",
                                "P6M19DT86399.999S",
                                "PT1715.714S"};

    testCommon(expectedResults, "/functions/date/interval_arithmetic.json", "/test_simple_date.json");
  }

  @Test
  public void testToChar() throws Exception {
    mockUsDateFormatSymbols();

    String expectedResults[] = {(new LocalDate(2008, 2, 23)).toString("yyyy-MMM-dd"),
                                (new LocalTime(12, 20, 30)).toString("HH mm ss"),
                                (new LocalDateTime(2008, 2, 23, 12, 0, 0)).toString("yyyy MMM dd HH:mm:ss")};
    testCommon(expectedResults, "/functions/date/to_char.json", "/test_simple_date.json");
  }

  @Test
  @Ignore("relies on particular time zone")
  public void testToDateType() throws Exception {
    String expectedResults[] = {"2008-02-23T00:00:00.000-08:00",
                                "1970-01-01T12:20:30.000-08:00",
                                "2008-02-23T12:00:00.000-08:00",
                                "2008-02-23T12:00:00.000-08:00"};

    testCommon(expectedResults, "/functions/date/to_date_type.json", "/test_simple_date.json");
  }

}
