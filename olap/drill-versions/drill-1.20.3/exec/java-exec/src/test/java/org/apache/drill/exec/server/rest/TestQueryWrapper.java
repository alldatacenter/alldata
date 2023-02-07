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
package org.apache.drill.exec.server.rest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.rest.QueryWrapper.RestQueryBuilder;
import org.apache.drill.exec.server.rest.RestQueryRunner.QueryResult;
import org.apache.drill.test.ClusterFixture;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestQueryWrapper extends RestServerTest {

  @BeforeClass
  public static void setupServer() throws Exception {
    startCluster(ClusterFixture.bareBuilder(dirTestWatcher)
      .clusterSize(1)
      .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true));
  }

  @Test
  public void testShowSchemas() throws Exception {
    QueryResult result = runQuery("SHOW SCHEMAS");
    assertEquals("COMPLETED", result.queryState);
    assertNotEquals(0, result.rows.size());
    assertEquals(1, result.columns.size());
    assertEquals(result.columns.iterator().next(), "SCHEMA_NAME");
  }

  @Test
  public void testImpersonationDisabled() throws Exception {
    try {
      runQuery(new RestQueryBuilder()
          .query("SHOW SCHEMAS")
          .userName("alfred")
          .build());
      fail("Should have thrown exception");
    } catch (UserException e) {
      assertThat(e.getMessage(), containsString("User impersonation is not enabled"));
    }
  }

  @Test
  public void testSpecifyDefaultSchema() throws Exception {
    QueryResult result = runQuery(
        new RestQueryBuilder()
          .query("SHOW FILES")
          .defaultSchema("dfs.tmp")
          .build());
    // SHOW FILES will fail if default schema is not provided
    assertEquals("COMPLETED", result.queryState);
  }

  protected QueryResult runQueryWithOption(String sql, String name, String value) throws Exception {
    Map<String, String> options = new HashMap<>();
    options.put(name, value);
    return runQuery(
        new RestQueryBuilder()
        .query(sql)
        .sessionOptions(options)
        .build());
  }

  @Test
  public void testOptionWithQuery() throws Exception {
    runOptionTest(ExecConstants.ENABLE_VERBOSE_ERRORS_KEY, "true");   // Boolean
    runOptionTest(ExecConstants.QUERY_MAX_ROWS, "10");                // Long
    runOptionTest(ExecConstants.TEXT_ESTIMATED_ROW_SIZE_KEY, "10.5"); // Double
    runOptionTest(ExecConstants.OUTPUT_FORMAT_OPTION, "json");        // String
  }

  public void runOptionTest(String option, String value) throws Exception {
    String query = String.format("SELECT val FROM sys.options WHERE `name`='%s'", option);
    String origValue = client.queryBuilder()
        .sql(query)
        .singletonString();
    assertNotEquals(origValue, value,
        "Not a valid test: new value is the same as the current value");
    QueryResult result = runQueryWithOption(query, option, value);
    assertEquals(1, result.rows.size());
    assertEquals(1, result.columns.size());
    assertEquals(value, result.rows.get(0).get("val"));
  }

  @Test
  public void testInvalidOptionName() throws Exception {
    try {
      runQueryWithOption("SHOW SCHEMAS", "xxx", "s");
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString("The option 'xxx' does not exist."));
    }
  }

  @Test
  public void testInvalidBooleanOption() {
    try {
      runQueryWithOption("SHOW SCHEMAS", ExecConstants.ENABLE_VERBOSE_ERRORS_KEY, "not a boolean");
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString("not a valid value"));
    }
  }

  @Test
  public void testInvalidLongOption() {
    try {
      runQueryWithOption("SHOW SCHEMAS", ExecConstants.QUERY_MAX_ROWS, "bogus");
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString("not a valid value"));
    }
  }

  @Test
  public void testInvalidDoubleOption() {
    try {
      runQueryWithOption("SHOW SCHEMAS", ExecConstants.TEXT_ESTIMATED_ROW_SIZE_KEY, "bogus");
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      assertThat(e.getMessage(), containsString("not a valid value"));
    }
  }
}
