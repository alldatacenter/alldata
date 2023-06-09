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
package org.apache.drill.exec.work.prepare;

import java.sql.Date;
import java.util.List;

import org.apache.drill.common.types.Types;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.exec.proto.UserProtos.PreparedStatement;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

/**
 * Tests for creating and executing prepared statements.
 */
public class TestPreparedStatementProvider extends PreparedStatementTestBase {

  /**
   * Simple query.
   * @throws Exception
   */
  @Test
  public void simple() throws Exception {
    String query = "SELECT * FROM cp.`region.json` ORDER BY region_id LIMIT 1";
    PreparedStatement preparedStatement = createPrepareStmt(query, false, null);

    List<ExpectedColumnResult> expMetadata = ImmutableList.of(
        new ExpectedColumnResult("region_id", "BIGINT", true, 20, 0, 0, true, Long.class.getName()),
        new ExpectedColumnResult("sales_city", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("sales_state_province", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("sales_district", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("sales_region", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("sales_country", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("sales_district_id", "BIGINT", true, 20, 0, 0, true, Long.class.getName())
    );

    verifyMetadata(expMetadata, preparedStatement.getColumnsList());

    testBuilder()
        .unOrdered()
        .preparedStatement(preparedStatement.getServerHandle())
        .baselineColumns("region_id", "sales_city", "sales_state_province", "sales_district",
            "sales_region", "sales_country", "sales_district_id")
        .baselineValues(0L, "None", "None", "No District", "No Region", "No Country", 0L)
        .go();
  }

  /**
   * Create a prepared statement for a query that has GROUP BY clause in it
   */
  @Test
  public void groupByQuery() throws Exception {
    String query = "SELECT sales_city, count(*) as cnt FROM cp.`region.json` " +
        "GROUP BY sales_city ORDER BY sales_city DESC LIMIT 1";
    PreparedStatement preparedStatement = createPrepareStmt(query, false, null);

    List<ExpectedColumnResult> expMetadata = ImmutableList.of(
        new ExpectedColumnResult("sales_city", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("cnt", "BIGINT", false, 20, 0, 0, true, Long.class.getName())
    );

    verifyMetadata(expMetadata, preparedStatement.getColumnsList());

    testBuilder()
        .unOrdered()
        .preparedStatement(preparedStatement.getServerHandle())
        .baselineColumns("sales_city", "cnt")
        .baselineValues("Yakima", 1L)
        .go();
  }

  /**
   * Create a prepared statement for a query that joins two tables and has ORDER BY clause.
   */
  @Test
  public void joinOrderByQuery() throws Exception {
    String query = "SELECT l.l_quantity, l.l_shipdate, o.o_custkey FROM cp.`tpch/lineitem.parquet` l JOIN cp.`tpch/orders.parquet` o " +
        "ON l.l_orderkey = o.o_orderkey LIMIT 2";

    PreparedStatement preparedStatement = createPrepareStmt(query, false, null);

    List<ExpectedColumnResult> expMetadata = ImmutableList.of(
        new ExpectedColumnResult("l_quantity", "DOUBLE", false, 24, 0, 0, true, Double.class.getName()),
        new ExpectedColumnResult("l_shipdate", "DATE", false, 10, 0, 0, false, Date.class.getName()),
        new ExpectedColumnResult("o_custkey", "INTEGER", false, 11, 0, 0, true, Integer.class.getName())
    );

    verifyMetadata(expMetadata, preparedStatement.getColumnsList());
  }

  /**
   * Pass an invalid query to the create prepare statement request and expect a parser failure.
   * @throws Exception
   */
  @Test
  public void invalidQueryParserError() throws Exception {
    createPrepareStmt("BLAH BLAH", true, ErrorType.PARSE);
  }

  /**
   * Pass an invalid query to the create prepare statement request and expect a validation failure.
   * @throws Exception
   */
  @Test
  public void invalidQueryValidationError() throws Exception {
    // CALCITE-1120 allows SELECT without from syntax.
    // So with this change the query fails with VALIDATION error.
    createPrepareStmt("SELECT * sdflkgdh", true,
        ErrorType.VALIDATION /* Drill returns incorrect error for parse error*/);
  }
}
