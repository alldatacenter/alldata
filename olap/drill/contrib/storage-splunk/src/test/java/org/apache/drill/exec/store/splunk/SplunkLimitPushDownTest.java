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

package org.apache.drill.exec.store.splunk;

import org.apache.drill.categories.SlowTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class})
public class SplunkLimitPushDownTest extends SplunkBaseTest {

  @Test
  public void testLimit() throws Exception {
    String sql = "SELECT * FROM splunk._audit LIMIT 5";
    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "maxRecords=5")
      .match();
  }

  @Test
  public void testLimitWithOrderBy() throws Exception {
    // Limit should not be pushed down for this example due to the sort
    String sql = "SELECT * FROM splunk._audit ORDER BY ip LIMIT 4";
    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "maxRecords=-1")
      .match();
  }

  @Test
  public void testLimitWithOffset() throws Exception {
    // Limit should be pushed down and include the offset
    String sql = "SELECT * FROM splunk._audit LIMIT 4 OFFSET 5";
    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "maxRecords=9")
      .match();
  }

  @Test
  public void testLimitWithFilter() throws Exception {
    String sql = "SELECT * FROM splunk._audit WHERE rating = 52.17 LIMIT 4";
    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "maxRecords=4")
      .match();
  }
}
