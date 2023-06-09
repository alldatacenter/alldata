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
package com.mapr.drill.maprdb.tests.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;

import org.apache.drill.PlanTestBase;
import org.apache.drill.SingleRowListener;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.util.VectorUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.json.Json;

import com.mapr.db.Table;
import com.mapr.db.impl.MapRDBImpl;
import com.mapr.db.tests.utils.DBTests;
import com.mapr.drill.maprdb.tests.MaprDBTestsSuite;
import com.mapr.tests.annotations.ClusterTest;

@Category(ClusterTest.class)
public class TestSimpleJson extends BaseJsonTest {

  private static final String TABLE_NAME = "business";
  private static final String JSON_FILE_URL = "/com/mapr/drill/json/business.json";

  private static boolean tableCreated = false;
  private static String tablePath;
  @Override
  protected String getTablePath() {
    return tablePath;
  }

  @BeforeClass
  public static void setup_TestSimpleJson() throws Exception {
    try (Table table = DBTests.createOrReplaceTable(TABLE_NAME);
         InputStream in = MaprDBTestsSuite.getJsonStream(JSON_FILE_URL);
         DocumentStream stream = Json.newDocumentStream(in)) {
      tableCreated = true;
      tablePath = table.getPath().toUri().getPath();

      for (Document document : stream) {
       table.insert(document, "business_id");
     }
     table.flush();
   }
  }

  @AfterClass
  public static void cleanup_TestEncodedFieldPaths() throws Exception {
    if (tableCreated) {
      DBTests.deleteTables(TABLE_NAME);
    }
  }

  @Test
  public void testSelectStar() throws Exception {
    final String sql = format("SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  %s.`%s` business");
    runSQLAndVerifyCount(sql, 10);
  }

  @Test
  public void testSelectId() throws Exception {
    setColumnWidths(new int[] {23});
    final String sql = format("SELECT\n"
        + "  _id\n"
        + "FROM\n"
        + "  %s.`%s` business");
    runSQLAndVerifyCount(sql, 10);
  }

  @Test
  public void testSelectNonExistentColumns() throws Exception {
    setColumnWidths(new int[] {23});
    final String sql = format("SELECT\n"
            + "  something\n"
            + "FROM\n"
            + "  %s.`%s` business limit 5");
    runSQLAndVerifyCount(sql, 5);
  }

  @Test
  public void testKVGen() throws Exception {
    setColumnWidths(new int[] {21, 10, 6});
    final String sql = format("select _id, t.parking[0].`key` K, t.parking[0].`value` V from"
        + " (select _id, kvgen(b.attributes.Parking) as parking from %s.`%s` b)"
        + " as t where t.parking[0].`key` = 'garage' AND t.parking[0].`value` = true");
    runSQLAndVerifyCount(sql, 1);
  }

  @Test
  public void testPushdownDisabled() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, categories, full_address\n"
        + "FROM\n"
        + "  table(%s.`%s`(type => 'maprdb', enablePushdown => false)) business\n"
        + "WHERE\n"
        + " name <> 'Sprint'");
    runSQLAndVerifyCount(sql, 9);

    final String[] expectedPlan = {"condition=null", "columns=\\[`\\*`\\]"};
    final String[] excludedPlan = {"condition=\\(name != \"Sprint\"\\)", "columns=\\[`name`, `_id`, `categories`, `full_address`\\]"};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushdownStringEqual() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, business.hours.Monday.`open`, categories[1], years[2], full_address\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " name = 'Sprint'");

    final Document queryResult = MapRDBImpl.newDocument();
    SingleRowListener listener = new SingleRowListener() {
      @Override
      protected void rowArrived(QueryDataBatch result) {
        final RecordBatchLoader loader = new RecordBatchLoader(getAllocator());
        loader.load(result.getHeader().getDef(), result.getData());
        StringBuilder sb = new StringBuilder();
        VectorUtil.appendVectorAccessibleContent(loader, sb, "|", false);
        loader.clear();
        queryResult.set("result", sb.toString());
      }
    };
    testWithListener(QueryType.SQL, sql, listener);
    listener.waitForCompletion();

    assertNull(queryResult.getString("error"));
    assertNotNull(queryResult.getString("result"));

    String[] fields = queryResult.getString("result").split("\\|");
    assertEquals("1970-01-01T11:00:00.000", fields[2]);
    assertEquals("Mobile Phones", fields[3]);
    assertEquals("2016.0", fields[4]);

    final String[] expectedPlan = {"condition=\\(name = \"Sprint\"\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushdownStringLike() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, categories, full_address\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " name LIKE 'S%%'");
    runSQLAndVerifyCount(sql, 3);

    final String[] expectedPlan = {"condition=\\(name MATCHES \"\\^\\\\\\\\QS\\\\\\\\E\\.\\*\\$\"\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushdownStringNotEqual() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, categories, full_address\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " name <> 'Sprint'");
    runSQLAndVerifyCount(sql, 9);

    final String[] expectedPlan = {"condition=\\(name != \"Sprint\"\\)", "columns=\\[`name`, `_id`, `categories`, `full_address`\\]"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushdownLongEqual() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, categories, full_address\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " zip = 85260");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(zip = \\{\"\\$numberLong\":85260\\}\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testCompositePredicate() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, categories, full_address\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " zip = 85260\n"
        + " OR\n"
        + " city = 'Las Vegas'");
    runSQLAndVerifyCount(sql, 4);

    final String[] expectedPlan = {"condition=\\(\\(zip = \\{\"\\$numberLong\":85260\\}\\) or \\(city = \"Las Vegas\"\\)\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPruneScanRange() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, categories, full_address\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " _id = 'jFTZmywe7StuZ2hEjxyA'");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(_id = \"jFTZmywe7StuZ2hEjxyA\"\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  @Ignore("Bug 27981")
  public void testPruneScanRangeAndPushDownCondition() throws Exception {
    // XXX/TODO:
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, categories, full_address\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " _id = 'jFTZmywe7StuZ2hEjxyA' AND\n"
        + " name = 'Subway'");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(\\(_id = \"jFTZmywe7StuZ2hEjxyA\"\\) and \\(name = \"Subway\"\\)\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownOnSubField1() throws Exception {
    setColumnWidths(new int[] {25, 120, 20});
    final String sql = format("SELECT\n"
        + "  _id, name, b.attributes.Ambience.touristy attributes\n"
        + "FROM\n"
        + "  %s.`%s` b\n"
        + "WHERE\n"
        + " b.attributes.Ambience.casual = false");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(attributes.Ambience.casual = false\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownOnSubField2() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, b.attributes.Attire attributes\n"
        + "FROM\n"
        + "  %s.`%s` b\n"
        + "WHERE\n"
        + " b.attributes.Attire = 'casual'");
    runSQLAndVerifyCount(sql, 4);

    final String[] expectedPlan = {"condition=\\(attributes.Attire = \"casual\"\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }
  @Test
  public void testPushDownIsNull() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});

    final String sql = format("SELECT\n"
        + "  _id, name, attributes\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " business.attributes.Ambience.casual IS NULL");
    runSQLAndVerifyCount(sql, 7);

    final String[] expectedPlan = {"condition=\\(\\(attributes.Ambience.casual = null\\) or \\(TYPE_OF\\(attributes.Ambience.casual\\) = NULL\\)\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownIsNotNull() throws Exception {
    setColumnWidths(new int[] {25, 75, 75, 50});

    final String sql = format("SELECT\n"
        + "  _id, name, b.attributes.Parking\n"
        + "FROM\n"
        + "  %s.`%s` b\n"
        + "WHERE\n"
        + " b.attributes.Ambience.casual IS NOT NULL");
    runSQLAndVerifyCount(sql, 3);

    final String[] expectedPlan = {"condition=\\(\\(attributes.Ambience.casual != null\\) and \\(TYPE_OF\\(attributes.Ambience.casual\\) != NULL\\)\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownOnSubField3() throws Exception {
    setColumnWidths(new int[] {25, 40, 40, 40});
    final String sql = format("SELECT\n"
        + "  _id, name, b.attributes.`Accepts Credit Cards` attributes\n"
        + "FROM\n"
        + "  %s.`%s` b\n"
        + "WHERE\n"
        + " b.attributes.`Accepts Credit Cards` IS NULL");
    runSQLAndVerifyCount(sql, 3);

    final String[] expectedPlan = {"condition=\\(\\(attributes.Accepts Credit Cards = null\\) or \\(TYPE_OF\\(attributes.Accepts Credit Cards\\) = NULL\\)\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownLong() throws Exception {
    final String sql = format("SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " stars > 4.0");
    runSQLAndVerifyCount(sql, 2);

    final String[] expectedPlan = {"condition=\\(stars > 4\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownSubField4() throws Exception {
    final String sql = format("SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " business.attributes.`Good For`.lunch = true AND"
        + " stars > 4.1");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(\\(attributes.Good For.lunch = true\\) and \\(stars > 4.1\\)\\)"};
    final String[] excludedPlan ={};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }


  @Test
  public void testPushDownSubField5() throws Exception {
    final String sql = format("SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " business.hours.Tuesday.`open` < TIME '10:30:00'");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(hours.Tuesday.open < \\{\"\\$time\":\"10:30:00\"\\}\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownSubField6() throws Exception {
    final String sql = format("SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " business.hours.Sunday.`close` > TIME '20:30:00'");
    runSQLAndVerifyCount(sql, 3);

    final String[] expectedPlan = {"condition=\\(hours.Sunday.close > \\{\"\\$time\":\"20:30:00\"\\}\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownSubField7() throws Exception {
    setColumnWidths(new int[] {25, 40, 25, 45});
    final String sql = format("SELECT\n"
        + "  _id, name, start_date, last_update\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " business.`start_date` = DATE '2012-07-14'");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(start_date = \\{\"\\$dateDay\":\"2012-07-14\"\\}\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testPushDownSubField8() throws Exception {
    setColumnWidths(new int[] {25, 40, 25, 45});
    final String sql = format("SELECT\n"
        + "  _id, name, start_date, last_update\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " business.`last_update` = TIMESTAMP '2012-10-20 07:42:46'");
    runSQLAndVerifyCount(sql, 1);

    final String[] expectedPlan = {"condition=\\(last_update = \\{\"\\$date\":\"2012-10-20T07:42:46.000Z\"\\}\\)"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
  }

  @Test
  public void testLimit() throws Exception {
    final String sql = format("SELECT\n"
        + "  _id, name, start_date, last_update\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "limit 1"
    );

    final String[] expectedPlan = {"JsonTableGroupScan.*limit=1"};
    final String[] excludedPlan = {};

    PlanTestBase.testPlanMatchingPatterns(sql, expectedPlan, excludedPlan);
    runSQLAndVerifyCount(sql, 1);
  }

}
