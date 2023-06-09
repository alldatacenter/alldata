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
package org.apache.drill.exec.sql;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.SqlTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SqlTest.class)
public class TestWithClause extends BaseTestQuery {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestWithClause.class);

  @Test
  public void withClause() throws Exception {
    test("with alpha as (select * from sys.options where optionScope = 'SYSTEM') \n" +
        "\n" +
        "select * from alpha");
  }

  @Test
  @Ignore
  public void withClauseWithAliases() throws Exception {
    test("with alpha (x,y) as (select name, kind from sys.options where optionScope = 'SYSTEM') \n" +
        "\n" +
        "select x, y from alpha");
  }

  @Test // DRILL-2318
  public void withClauseOrderBy() throws Exception {
    String query = "WITH x \n" +
        "AS (SELECT n_nationkey a1 \n" +
        "FROM  cp.`tpch/nation.parquet`) \n" +
        "SELECT  x.a1 \n" +
        "FROM  x \n" +
        "ORDER BY x.a1 \n" +
        "limit 5";

    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("a1")
      .baselineValues(0)
      .baselineValues(1)
      .baselineValues(2)
      .baselineValues(3)
      .baselineValues(4)
      .build()
      .run();
  }
}
