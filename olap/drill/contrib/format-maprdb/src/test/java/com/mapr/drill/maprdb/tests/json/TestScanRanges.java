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
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.user.AwaitableUserResultsListener;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.json.Json;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import com.mapr.db.Table;
import com.mapr.db.tests.utils.DBTests;
import com.mapr.drill.maprdb.tests.MaprDBTestsSuite;
import com.mapr.tests.annotations.ClusterTest;

@Category(ClusterTest.class)
public class TestScanRanges extends BaseJsonTest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestScanRanges.class);

  private static final int TOTAL_ROW_COUNT = 1000000;
  private static final String TABLE_NAME = "large_table_TestScanRanges";
  private static final String JSON_FILE_URL = "/com/mapr/drill/json/business.json";

  private static boolean tableCreated = false;
  private static String tablePath;
  protected String getTablePath() {
    return tablePath;
  }

  @BeforeClass
  public static void setup_TestSimpleJson() throws Exception {
    // We create a large table with auto-split set to disabled.
    // Without intra-tablet partitioning, this test should run with only one minor fragment
    try (Table table = DBTests.createOrReplaceTable(TABLE_NAME, false /*autoSplit*/);
         InputStream in = MaprDBTestsSuite.getJsonStream(JSON_FILE_URL);
         DocumentStream stream = Json.newDocumentStream(in)) {
      tableCreated = true;
      tablePath = table.getPath().toUri().getPath();

      List<Document> docs = Lists.newArrayList(stream);
      for (char ch = 'A'; ch <= 'T'; ch++) {
        for (int rowIndex = 0; rowIndex < 5000; rowIndex++) {
          for (int i = 0; i < docs.size(); i++) {
            final Document document = docs.get(i);
            final String id = String.format("%c%010d%03d", ch, rowIndex, i);
            document.set("documentId", rowIndex);
            table.insertOrReplace(id, document);
          }
        }
      }
      table.flush();
      DBTests.waitForRowCount(table.getPath(), TOTAL_ROW_COUNT);

      setSessionOption("planner.width.max_per_node", 5);
    }
  }

  @AfterClass
  public static void cleanup_TestEncodedFieldPaths() throws Exception {
    if (tableCreated) {
      DBTests.deleteTables(TABLE_NAME);
    }
  }

  @Test
  public void test_scan_ranges() throws Exception {
    final PersistentStore<UserBitShared.QueryProfile> completed = getDrillbitContext().getProfileStoreContext().getCompletedProfileStore();

    setColumnWidths(new int[] {25, 40, 25, 45});
    final String sql = format("SELECT\n"
        + "  *\n"
        + "FROM\n"
        + "  %s.`%s` business");

    final SilentListener resultListener = new SilentListener();
    final AwaitableUserResultsListener listener = new AwaitableUserResultsListener(resultListener);
    testWithListener(QueryType.SQL, sql, listener);
    listener.await();

    assertEquals(TOTAL_ROW_COUNT, resultListener.getRowCount());
    String queryId = QueryIdHelper.getQueryId(resultListener.getQueryId());

    QueryProfile profile = completed.get(queryId);
    String profileString = String.valueOf(profile);
    logger.debug(profileString);
    assertNotNull(profile);
    assertTrue(profile.getTotalFragments() >= 5); // should at least as many as
  }

  @Test
  public void test_scan_ranges_with_filter_on_id() throws Exception {
    setColumnWidths(new int[] {25, 25, 25});
    final String sql = format("SELECT\n"
        + "  _id, business_id, city\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " _id > 'M' AND _id < 'Q'");

    final SilentListener resultListener = new SilentListener();
    final AwaitableUserResultsListener listener = new AwaitableUserResultsListener(resultListener);
    testWithListener(QueryType.SQL, sql, listener);
    listener.await();

    assertEquals(200000, resultListener.getRowCount());
  }

  @Test
  public void test_scan_ranges_with_filter_on_non_id_field() throws Exception {
    setColumnWidths(new int[] {25, 25, 25});
    final String sql = format("SELECT\n"
        + "  _id, business_id, documentId\n"
        + "FROM\n"
        + "  %s.`%s` business\n"
        + "WHERE\n"
        + " documentId >= 100 AND documentId < 150");

    final SilentListener resultListener = new SilentListener();
    final AwaitableUserResultsListener listener = new AwaitableUserResultsListener(resultListener);
    testWithListener(QueryType.SQL, sql, listener);
    listener.await();

    assertEquals(10000, resultListener.getRowCount());
  }

}
