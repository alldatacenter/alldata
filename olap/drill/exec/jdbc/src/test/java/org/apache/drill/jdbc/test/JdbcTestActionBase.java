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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.drill.test.TestTools;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.jdbc.JdbcTestBase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

public class JdbcTestActionBase extends JdbcTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JdbcTestActionBase.class);

  // Set a timeout unless we're debugging.
  static Connection connection;

  static {
    Driver.load();
  }

  @Rule
  public TestRule TIMEOUT = TestTools.getTimeoutRule(40000);

  @BeforeClass
  public static void openClient() throws Exception {
    connection = connect();
  }

  @AfterClass
  public static void closeClient() throws IOException, SQLException {
    connection.close();
  }

  protected void testAction(JdbcAction action) throws Exception {
    testAction(action, -1);
  }

  protected void testAction(JdbcAction action, long rowcount) throws Exception {
    final StringBuilder sb = new StringBuilder();

    int rows = 0;
    Stopwatch watch = Stopwatch.createStarted();
    ResultSet r = action.getResult(connection);
    boolean first = true;
    while (r.next()) {
      rows++;
      ResultSetMetaData md = r.getMetaData();
      if (first == true) {
        for (int i = 1; i <= md.getColumnCount(); i++) {
          sb.append(md.getColumnName(i));
          sb.append('\t');
        }
        sb.append('\n');
        first = false;
      }

      for (int i = 1; i <= md.getColumnCount(); i++) {
        sb.append(r.getObject(i));
        sb.append('\t');
      }
      sb.append('\n');
    }

    sb.append(String.format("Query completed in %d millis.\n", watch.elapsed(TimeUnit.MILLISECONDS)));

    if (rowcount != -1) {
      Assert.assertEquals((long) rowcount, (long) rows);
    }

    sb.append("\n\n\n");
    logger.info(sb.toString());
  }

  public interface JdbcAction {
    ResultSet getResult(Connection c) throws SQLException;
  }
}
