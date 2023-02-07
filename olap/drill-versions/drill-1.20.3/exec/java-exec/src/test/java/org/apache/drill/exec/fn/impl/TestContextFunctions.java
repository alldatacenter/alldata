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
package org.apache.drill.exec.fn.impl;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.SqlFunctionTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SqlFunctionTest.class)
public class TestContextFunctions extends BaseTestQuery {

  @Test
  public void userUDFForAnonymousConnection() throws Exception {
    updateClient("");
    testBuilder()
        .sqlQuery("select user, session_user, system_user from cp.`employee.json` limit 1")
        .unOrdered()
        .baselineColumns("user", "session_user", "system_user")
        .baselineValues("anonymous", "anonymous", "anonymous")
        .go();
  }

  @Test
  public void userUDFForNamedConnection() throws Exception {
    final String testUserName = "testUser1";
    updateClient(testUserName);
    testBuilder()
        .sqlQuery("select user, session_user, system_user from cp.`employee.json` limit 1")
        .unOrdered()
        .baselineColumns("user", "session_user", "system_user")
        .baselineValues(testUserName, testUserName, testUserName)
        .go();
  }

  @Test
  public void userUDFInFilterCondition() throws Exception {
    final String testUserName = "testUser2";
    updateClient(testUserName);
    final String query = String.format(
        "select employee_id from cp.`employee.json` where '%s' = user order by employee_id limit 1", testUserName);
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("employee_id")
        .baselineValues(1L)
        .go();
  }

  @Test
  public void currentSchemaUDFWhenDefaultSchemaNotSet() throws Exception {
    testBuilder()
        .sqlQuery("select current_schema from cp.`employee.json` limit 1")
        .unOrdered()
        .baselineColumns("current_schema")
        .baselineValues("")
        .go();
  }

  @Test
  public void currentSchemaUDFWithSingleLevelDefaultSchema() throws Exception {
    testBuilder()
        .optionSettingQueriesForTestQuery("USE dfs")
        .sqlQuery("select current_schema from cp.`employee.json` limit 1")
        .unOrdered()
        .baselineColumns("current_schema")
        .baselineValues("dfs")
        .go();
  }

  @Test
  public void currentSchemaUDFWithMultiLevelDefaultSchema() throws Exception {
    testBuilder()
        .optionSettingQueriesForTestQuery("USE dfs.tmp")
        .sqlQuery("select current_schema from cp.`employee.json` limit 1")
        .unOrdered()
        .baselineColumns("current_schema")
        .baselineValues("dfs.tmp")
        .go();
  }

  @Test
  public void sessionIdUDFWithinSameSession() throws Exception {
    final String sessionIdQuery = "select session_id as sessionId from (values(1))";
    testBuilder()
        .sqlQuery(sessionIdQuery)
        .ordered()
        .sqlBaselineQuery(sessionIdQuery)
        .build()
        .run();
  }
}
