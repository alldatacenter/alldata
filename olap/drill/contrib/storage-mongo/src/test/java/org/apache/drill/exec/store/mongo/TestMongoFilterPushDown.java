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
public class TestMongoFilterPushDown extends MongoTestBase {

  @Test
  public void testFilterPushDownIsEqual() throws Exception {
    String queryString = String.format(TEST_FILTER_PUSH_DOWN_EQUAL_QUERY_TEMPLATE_1, EMPLOYEE_DB, EMPINFO_COLLECTION);

    testBuilder()
        .sqlQuery(queryString)
        .unOrdered()
        .expectsNumRecords(1)
        .go();
  }

  @Test
  public void testFilterPushDownLessThanWithSingleField() throws Exception {
    String queryString = String.format(TEST_FILTER_PUSH_DOWN_LESS_THAN_QUERY_TEMPLATE_1, EMPLOYEE_DB,
        EMPINFO_COLLECTION);

    testBuilder()
        .sqlQuery(queryString)
        .unOrdered()
        .expectsNumRecords(9)
        .go();
  }

  @Test
  public void testFilterPushDownGreaterThanWithSingleField() throws Exception {
    String queryString = String.format(TEST_FILTER_PUSH_DOWN_GREATER_THAN_QUERY_TEMPLATE_1, EMPLOYEE_DB,
        EMPINFO_COLLECTION);

    testBuilder()
        .sqlQuery(queryString)
        .unOrdered()
        .expectsNumRecords(9)
        .go();
  }

  @Test
  public void testFilterPushDownIsNull() throws Exception {
    String queryString = String.format(TEST_FILTER_PUSH_DOWN_IS_NULL_QUERY_TEMPLATE_1, EMPLOYEE_DB, EMPINFO_COLLECTION);

    testBuilder()
        .sqlQuery(queryString)
        .unOrdered()
        .expectsNumRecords(2)
        .go();
  }

  @Test
  public void testFilterPushDownIsNotNull() throws Exception {
    String queryString = String.format(TEST_FILTER_PUSH_DOWN_IS_NOT_NULL_QUERY_TEMPLATE_1, EMPLOYEE_DB, EMPINFO_COLLECTION);

    testBuilder()
        .sqlQuery(queryString)
        .unOrdered()
        .expectsNumRecords(17)
        .go();
  }
}
