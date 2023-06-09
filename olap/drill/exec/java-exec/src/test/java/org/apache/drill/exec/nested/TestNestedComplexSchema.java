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
package org.apache.drill.exec.nested;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.UnlikelyTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestNestedComplexSchema extends BaseTestQuery {

  @Test
  public void testNested1() throws Exception {
    test("select tbl.arrayval[0] from cp.`nested/nested_1.json` tbl");
  }

  @Test
  public void testNested2() throws Exception {
    test("select tbl.a.arrayval[0] from cp.`nested/nested_2.json` tbl");
  }

  @Test
  public void testNested3() throws Exception {
    test("select tbl.a.arrayval[0].val1[0] from cp.`nested/nested_3.json` tbl");
  }

  @Test //DRILL-1649
  @Category(UnlikelyTest.class)
  public void testNestedFlattenWithJoin() throws Exception {

    final String query="" +
    "  select event_info.uid, transaction_info.trans_id, event_info.event.evnt_id  evnt_id "+
    "from ( "+
    "  select userinfo.transaction.trans_id trans_id, max(userinfo.event.event_time) max_event_time "+
    "  from ( "+
      "    select uid, flatten(events) event, flatten(transactions) transaction from cp.`complex/json/single-user-transactions.json` "+
    ") userinfo "+
    "where userinfo.transaction.trans_time >= userinfo.event.event_time "+
    "group by userinfo.transaction.trans_id "+
    ") transaction_info "+
    "inner join "+
    "( "+
    "  select uid, flatten(events) event "+
    "  from cp.`complex/json/single-user-transactions.json` "+
    ") event_info "+
    "on transaction_info.max_event_time = event_info.event.event_time "+
    "";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .jsonBaselineFile("complex/drill-1649-result.json")
      .go();
  }


}
