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
package org.apache.drill.jdbc.test;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.drill.test.TestTools;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.jdbc.JdbcTestBase;
import org.apache.drill.categories.JdbcTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@Category(JdbcTest.class)
public class TestJdbcDistQuery extends JdbcTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestJdbcDistQuery.class);

  // Set a timeout unless we're debugging.
  @Rule
  public TestRule TIMEOUT = TestTools.getTimeoutRule(50000);

  static{
    Driver.load();
  }

  @BeforeClass
  public static void setup() {
    dirTestWatcher.copyFileToRoot(Paths.get("sample-data"));
  }

  // TODO:  Purge nextUntilEnd(...) and calls when remaining fragment race
  // conditions are fixed (not just DRILL-2245 fixes).
  /**
   * Calls {@link ResultSet#next} on given {@code ResultSet} until it returns
   * false.  (For TEMPORARY workaround for query cancelation race condition.)
   */
  private static void nextUntilEnd(final ResultSet resultSet) throws SQLException {
    while (resultSet.next()) {
    }
  }

  @Test
  public void testSimpleQuerySingleFile() throws Exception{
    testQuery("select R_REGIONKEY, R_NAME from dfs.`sample-data/regionsSF/`");
  }

  @Test
  public void testSimpleQueryMultiFile() throws Exception{
    testQuery("select R_REGIONKEY, R_NAME from dfs.`sample-data/regionsMF/`");
  }

  @Test
  public void testWhereOverSFile() throws Exception{
    testQuery("select R_REGIONKEY, R_NAME from dfs.`sample-data/regionsSF/` WHERE R_REGIONKEY = 1");
  }

  @Test
  public void testWhereOverMFile() throws Exception{
    testQuery("select R_REGIONKEY, R_NAME from dfs.`sample-data/regionsMF/` WHERE R_REGIONKEY = 1");
  }


  @Test
  public void testAggSingleFile() throws Exception{
    testQuery("select R_REGIONKEY from dfs.`sample-data/regionsSF/` group by R_REGIONKEY");
  }

  @Test
  public void testAggMultiFile() throws Exception{
    testQuery("select R_REGIONKEY from dfs.`sample-data/regionsMF/` group by R_REGIONKEY");
  }

  @Test
  public void testAggOrderByDiffGKeyMultiFile() throws Exception{
    testQuery("select R_REGIONKEY, SUM(cast(R_REGIONKEY AS int)) As S "
        + "from dfs.`sample-data/regionsMF/` "
        + "group by R_REGIONKEY ORDER BY S");
  }

  @Test
  public void testAggOrderBySameGKeyMultiFile() throws Exception{
    testQuery("select R_REGIONKEY, SUM(cast(R_REGIONKEY AS int)) As S "
        + "from dfs.`sample-data/regionsMF/` "
        + "group by R_REGIONKEY "
        + "ORDER BY R_REGIONKEY");
  }

  @Ignore
  @Test
  public void testJoinSingleFile() throws Exception{
    testQuery("select T1.R_REGIONKEY "
        + "from dfs.`sample-data/regionsSF/` as T1 "
        + "join dfs.`sample-data/nationsSF/` as T2 "
        + "on T1.R_REGIONKEY = T2.N_REGIONKEY");
  }

  @Ignore
  @Test
  public void testJoinMultiFile() throws Exception{
    testQuery("select T1.R_REGIONKEY "
        + "from dfs.`sample-data/regionsMF/` as T1 "
        + "join dfs.`sample-data/nationsMF/` as T2 "
        + "on T1.R_REGIONKEY = T2.N_REGIONKEY");
  }

  @Ignore
  @Test
  public void testJoinMFileWhere() throws Exception{
    testQuery("select T1.R_REGIONKEY, T1.R_NAME "
        + "from dfs.`sample-data/regionsMF/` as T1 "
        + "join dfs.`sample-data/nationsMF/` as T2 "
        + "on T1.R_REGIONKEY = T2.N_REGIONKEY "
        + "WHERE T1.R_REGIONKEY  = 3 ");
  }

  @Test
  //NPE at ExternalSortBatch.java : 151
  public void testSortSingleFile() throws Exception{
    testQuery("select R_REGIONKEY "
        + "from dfs.`sample-data/regionsSF/` "
        + "order by R_REGIONKEY");
  }

  @Test
  //NPE at ExternalSortBatch.java : 151
  public void testSortMultiFile() throws Exception{
    testQuery("select R_REGIONKEY "
        + "from dfs.`sample-data/regionsMF/` "
        + "order by R_REGIONKEY");
  }

  @Test
  public void testSortMFileWhere() throws Exception{
    testQuery("select R_REGIONKEY "
        + "from dfs.`sample-data/regionsMF/` "
        + "WHERE R_REGIONKEY = 1 "
        + "order by R_REGIONKEY");
  }

  @Ignore
  @Test
  public void testJoinAggSortWhere() throws Exception{
    testQuery("select T1.R_REGIONKEY, COUNT(1) as CNT "
        + "from dfs.`sample-data/regionsMF/` as T1 "
        + "join dfs.`sample-data/nationsMF/` as T2 "
        + "on T1.R_REGIONKEY = T2.N_REGIONKEY "
        + "WHERE T1.R_REGIONKEY  = 3 "
        + "GROUP BY T1.R_REGIONKEY "
        + "ORDER BY T1.R_REGIONKEY");
  }

  @Test
  public void testSelectLimit() throws Exception{
    testQuery("select R_REGIONKEY, R_NAME "
        + "from dfs.`sample-data/regionsMF/` "
        + "limit 2");
  }

  private void testQuery(String sql) throws Exception {
    final StringBuilder sb = new StringBuilder();
    boolean success = false;
    try (Connection c = connect()) {
      // ???? TODO:  What is this currently redundant one-time loop for?  (If
      // it's kept around to make it easy to switch to looping multiple times
      // (e.g., for debugging) then define a constant field or local variable
      // for the number of iterations.)
      boolean first = true;
      for (int x = 0; x < 1; x++) {
        Stopwatch watch = Stopwatch.createStarted();
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery(sql);
        ResultSetMetaData md = r.getMetaData();
        if (first) {
          for (int i = 1; i <= md.getColumnCount(); i++) {
            sb.append(md.getColumnName(i));
            sb.append('\t');
          }
          sb.append('\n');
          first = false;
        }
        while (r.next()) {
          md = r.getMetaData();

          for (int i = 1; i <= md.getColumnCount(); i++) {
            sb.append(r.getObject(i));
            sb.append('\t');
          }
          sb.append('\n');
        }

        sb.append(String.format("Query completed in %d millis.\n", watch.elapsed(TimeUnit.MILLISECONDS)));
      }

      sb.append("\n\n\n");
      success = true;
    } finally {
      if (!success) {
        Thread.sleep(2000);
      }
    }

    logger.info(sb.toString());
  }

  @Test
  public void testSchemaForEmptyResultSet() throws Exception {
    String query = "select fullname, occupation, postal_code from cp.`customer.json` where 0 = 1";
    try (Connection c = connect()) {
      Statement s = c.createStatement();
      ResultSet r = s.executeQuery(query);
      ResultSetMetaData md = r.getMetaData();
      List<String> columns = Lists.newArrayList();
      for (int i = 1; i <= md.getColumnCount(); i++) {
        columns.add(md.getColumnName(i));
      }
      String[] expected = {"fullname", "occupation", "postal_code"};
      Assert.assertEquals(3, md.getColumnCount());
      Assert.assertArrayEquals(expected, columns.toArray());
      // TODO:  Purge nextUntilEnd(...) and calls when remaining fragment race
      // conditions are fixed (not just DRILL-2245 fixes).
      nextUntilEnd(r);
    }
  }
}
