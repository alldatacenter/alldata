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
package org.apache.drill;

import org.junit.Test;

public class TestCTASJson extends PlanTestBase {

  /**
   * Test a source json file that contains records that are maps with fields of
   * all types. Some records have missing fields. CTAS should skip the missing
   * fields
   */
  @Test
  public void testctas_alltypes_map() throws Exception {
    String testName = "ctas_alltypes_map";
    test("use dfs.tmp");
    test("create table " + testName + "_json as select * from cp.`json/" + testName + ".json`");

    final String query = "select * from `" + testName + "_json` t1 ";

    try {
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .jsonBaselineFile("json/" + testName + ".json")
          .optionSettingQueriesForTestQuery("alter session set `store.format` = 'json'")
          .optionSettingQueriesForTestQuery("alter session set store.json.writer.skip_null_fields = true") // DEFAULT
          .build()
          .run();
    } finally {
      test("drop table " + testName + "_json");
      resetSessionOption("store.format");
      resetSessionOption("store.json.writer.skip_null_fields");
    }
  }

  /**
   * Test a source json file that contains records that are maps with fields of
   * all types. Some records have missing fields. CTAS should NOT skip the
   * missing fields
   */
  @Test
  public void testctas_alltypes_map_noskip() throws Exception {
    String testName = "ctas_alltypes_map";
    test("use dfs.tmp");
    test("create table " + testName + "_json as select * from cp.`json/" + testName + ".json`");

    final String query = "select * from `" + testName + "_json` t1 ";

    try {
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .jsonBaselineFile("json/" + testName + "_out.json")
          .optionSettingQueriesForTestQuery("alter session set `store.format` = 'json'")
          .optionSettingQueriesForTestQuery("alter session set store.json.writer.skip_null_fields = false") // change from DEFAULT
          .build()
          .run();
    } finally{
      test("drop table " + testName + "_json" );
      resetSessionOption("store.format");
      resetSessionOption("store.json.writer.skip_null_fields");
    }
  }

  /**
   * Test a source json file that contains records that are maps with fields of
   * all types. Some records have missing fields. CTAS should skip the missing
   * fields
   */
  @Test
  public void testctas_alltypes_repeatedmap() throws Exception {
    String testName = "ctas_alltypes_repeated_map";
    test("use dfs.tmp");
    test("create table " + testName + "_json as select * from cp.`json/" + testName + ".json`");

    final String query = "select * from `" + testName + "_json` t1 ";

    try {
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .jsonBaselineFile("json/" + testName + ".json")
          .optionSettingQueriesForTestQuery("alter session set `store.format` = 'json'")
          .optionSettingQueriesForTestQuery(
              "alter session set store.json.writer.skip_null_fields = true") // DEFAULT
          .build()
          .run();
    }finally{
      test("drop table " + testName + "_json" );
      resetSessionOption("store.format");
      resetSessionOption("store.json.writer.skip_null_fields");
    }
  }

  /**
   * Test a source json file that contains records that are maps with fields of
   * all types. Some records have missing fields. CTAS should NOT skip the
   * missing fields
   */
  @Test
  public void testctas_alltypes_repeated_map_noskip() throws Exception {
    String testName = "ctas_alltypes_repeated_map";
    test("use dfs.tmp");
    test("create table " + testName + "_json as select * from cp.`json/" + testName + ".json`");

    final String query = "select * from `" + testName + "_json` t1 ";

    try {
      testBuilder()
          .sqlQuery(query)
          .ordered()
          .jsonBaselineFile("json/" + testName + "_out.json")
          .optionSettingQueriesForTestQuery("alter session set `store.format` = 'json'")
          .optionSettingQueriesForTestQuery(
              "alter session set store.json.writer.skip_null_fields = false") // change from DEFAULT
          .build()
          .run();
    } finally {
      test("drop table " + testName + "_json" );
      resetSessionOption("store.format");
      resetSessionOption("store.json.writer.skip_null_fields");
    }
  }
}
