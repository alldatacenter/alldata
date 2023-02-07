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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.drill.test.TestTools;
import org.apache.drill.jdbc.DrillResultSet;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.jdbc.JdbcTestBase;
import org.junit.Rule;
import org.junit.rules.TestRule;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

public class JdbcTestQueryBase extends JdbcTestBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JdbcTestQueryBase.class);

  // Set a timeout unless we're debugging.
  @Rule
  public TestRule TIMEOUT = TestTools.getTimeoutRule(40000);

  static{
    Driver.load();
  }

  protected static void testQuery(String sql) throws Exception {
    final StringBuilder sb = new StringBuilder();

    boolean success = false;
    try (Connection conn = connect()) {
      for (int x = 0; x < 1; x++) {
        Stopwatch watch = Stopwatch.createStarted();
        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery(sql);
        sb.append(String.format("QueryId: %s\n", r.unwrap(DrillResultSet.class).getQueryId()));
        boolean first = true;
        while (r.next()) {
          ResultSetMetaData md = r.getMetaData();
          if (first == true) {
            for (int i = 1; i <= md.getColumnCount(); i++) {
              sb.append(md.getColumnName(i));
              sb.append('\t');
            }
            sb.append('\b');
            first = false;
          }

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
}
