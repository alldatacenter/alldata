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
public class TestMongoDrillIssue extends MongoTestBase {

  // DRILL-7820: Not support database name in capital letters.
  @Test
  public void testIssue7820() throws Exception {
    testBuilder()
    .sqlQuery("show tables from mongo.ISSUE7820")
    .unOrdered()
    .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
    .baselineValues("mongo.issue7820", "Issue7820")
    .go();
  }

  // Add more test for DRILL-7820
  @Test
  public void testIssue7820Select() throws Exception {
    testBuilder()
    .sqlQuery("select employee_id, full_name from mongo.ISSUE7820.Issue7820 limit 5 offset 15")
    .unOrdered()
    .baselineColumns("employee_id", "full_name")
    .baselineValues(1116, "Phil Munoz")
    .baselineValues(1117, "Lori Lightfoot")
    .baselineValues(1, "Kumar")
    .baselineValues(2, "Kamesh")
    .go();
  }
}
