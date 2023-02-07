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

package org.apache.drill.exec.store.druid;

import org.apache.drill.categories.DruidStorageTest;
import org.apache.drill.categories.SlowTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Ignore("These tests require a running druid instance. You may start druid by using the docker-compose provide in resources/druid and enable these tests")
@Category({SlowTest.class, DruidStorageTest.class})
public class TestDruidQueries extends DruidTestBase {

  @Test
  public void testEqualsFilter() throws Exception {
    testBuilder()
      .sqlQuery(String.format(TEST_STRING_EQUALS_FILTER_QUERY_TEMPLATE1, TEST_DATASOURCE_WIKIPEDIA))
      .unOrdered()
      .expectsNumRecords(2)
      .go();
  }

  @Test
  public void testTwoANDdEqualsFilter() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_STRING_TWO_AND_EQUALS_FILTER_QUERY_TEMPLATE1, TEST_DATASOURCE_WIKIPEDIA))
        .unOrdered()
        .expectsNumRecords(1)
        .go();
  }

  @Test
  public void testTwoOrdEqualsFilter() throws Exception {
    testBuilder()
        .sqlQuery(String.format(TEST_STRING_TWO_OR_EQUALS_FILTER_QUERY_TEMPLATE1, TEST_DATASOURCE_WIKIPEDIA))
        .unOrdered()
        .expectsNumRecords(3)
        .go();
  }

  @Test
  public void testSingleColumnProject() throws Exception {
    String query = String.format(TEST_QUERY_PROJECT_PUSH_DOWN_TEMPLATE_1, TEST_DATASOURCE_WIKIPEDIA);

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("comment")
        .expectsNumRecords(24433)
        .go();
  }

  @Test
  public void testCountAllRowsQuery() throws Exception {
    String query = String.format(TEST_QUERY_COUNT_QUERY_TEMPLATE, TEST_DATASOURCE_WIKIPEDIA);

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("mycount")
      .baselineValues(24433L)
      .go();
  }
}
