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

package org.apache.drill.exec.store.mongo;

import org.apache.drill.categories.MongoStorageTest;
import org.apache.drill.categories.SlowTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({SlowTest.class, MongoStorageTest.class})
public class TestMongoLimitPushDown extends MongoTestBase {

  @Test
  public void testLimit() throws Exception {
    String sql = "SELECT `employee_id` FROM mongo.employee.`empinfo` LIMIT 4";
    queryBuilder()
        .sql(sql)
        .planMatcher()
        .exclude("Limit\\(")
        .include("MongoGroupScan.*\"\\$limit\": 4")
        .match();
  }

  @Test
  public void testLimitWithOrderBy() throws Exception {
    String sql = "SELECT `employee_id` FROM mongo.employee.`empinfo` ORDER BY employee_id LIMIT 4";
    queryBuilder()
      .sql(sql)
      .planMatcher()
      .exclude("Limit")
      .include("MongoGroupScan.*\"\\$sort\": \\{\"employee_id\": 1}", "\"\\$limit\": 4")
      .match();
  }

  @Test
  public void testLimitWithOffset() throws Exception {
    String sql = "SELECT `employee_id` FROM mongo.employee.`empinfo` LIMIT 4 OFFSET 5";
    queryBuilder()
      .sql(sql)
      .planMatcher()
      .exclude("Limit")
      .include("\"\\$skip\": 5", "\"\\$limit\": 4")
      .match();
  }

  @Test
  public void testLimitWithFilter() throws Exception {
    String sql = "SELECT `employee_id` FROM mongo.employee.`empinfo` WHERE rating = 52.17 LIMIT 4";
    queryBuilder()
      .sql(sql)
      .planMatcher()
      .exclude("Limit")
      .include("\"\\$limit\": 4", "\"\\$eq\": 52\\.17")
      .match();
  }

  @Test
  public void testSelectStarWithLimit() throws Exception {
    testBuilder()
      .sqlQuery("SELECT * FROM mongo.employee.`empinfo` LIMIT 4")
      .unOrdered()
      .expectsNumRecords(4)
      .go();
  }
}
