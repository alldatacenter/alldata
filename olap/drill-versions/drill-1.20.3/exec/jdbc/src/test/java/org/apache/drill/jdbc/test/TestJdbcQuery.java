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
package org.apache.drill.jdbc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

import org.apache.drill.categories.JdbcTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.jdbc.JdbcTestBase;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.junit.experimental.categories.Category;

@Category(JdbcTest.class)
public class TestJdbcQuery extends JdbcTestQueryBase {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestJdbcQuery.class);

  // TODO:  Purge nextUntilEnd(...) and calls when remaining fragment race
  // conditions are fixed (not just DRILL-2245 fixes).
  ///**
  // * Calls {@link ResultSet#next} on given {@code ResultSet} until it returns
  // * false.  (For TEMPORARY workaround for query cancelation race condition.)
  // */
  //private void nextUntilEnd(final ResultSet resultSet) throws SQLException {
  //  while (resultSet.next()) {
  //  }
  //}

  @BeforeClass
  public static void setupFiles() {
    dirTestWatcher.copyFileToRoot(Paths.get("sample-data"));
  }

  @Test // DRILL-3635
  public void testFix3635() throws Exception {
    // When we run a CTAS query, executeQuery() should return after the table has been flushed to disk even though we
    // didn't yet receive a terminal message. To test this, we run CTAS then immediately run a query on the newly
    // created table.

    final String tableName = "dfs.tmp.`testDDLs3635`";

    try (Connection conn = connect()) {
      Statement s = conn.createStatement();
      s.executeQuery(String.format("CREATE TABLE %s AS SELECT * FROM cp.`employee.json`", tableName));
    }

    testQuery(String.format("SELECT * FROM %s LIMIT 1", tableName));
  }

  @Test
  @Ignore
  public void testJsonQuery() throws Exception{
    testQuery("select * from cp.`employee.json`");
  }

  @Test
  public void testCast() throws Exception{
    testQuery("select R_REGIONKEY, cast(R_NAME as varchar(15)) as region, cast(R_COMMENT as varchar(255)) as comment from dfs.`sample-data/region.parquet`");
  }

  @Test
  @Ignore
  public void testWorkspace() throws Exception{
    testQuery("select * from dfs.root.`sample-data/region.parquet`");
  }

  @Test
  @Ignore
  public void testWildcard() throws Exception{
    testQuery("select * from dfs.`sample-data/region.parquet`");
  }

  @Test
  public void testCharLiteral() throws Exception {
    testQuery("select 'test literal' from INFORMATION_SCHEMA.`TABLES` LIMIT 1");
  }

  @Test
  public void testVarCharLiteral() throws Exception {
    testQuery("select cast('test literal' as VARCHAR) from INFORMATION_SCHEMA.`TABLES` LIMIT 1");
  }

  @Test
  @Ignore
  public void testLogicalExplain() throws Exception{
    testQuery("EXPLAIN PLAN WITHOUT IMPLEMENTATION FOR select * from dfs.`sample-data/region.parquet`");
  }

  @Test
  @Ignore
  public void testPhysicalExplain() throws Exception{
    testQuery("EXPLAIN PLAN FOR select * from dfs.`sample-data/region.parquet`");
  }

  @Test
  @Ignore
  public void checkUnknownColumn() throws Exception{
    testQuery("SELECT unknownColumn FROM dfs.`sample-data/region.parquet`");
  }

  @Test
  public void testLikeNotLike() throws Exception{
    withNoDefaultSchema()
      .sql("SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_NAME NOT LIKE 'C%' AND COLUMN_NAME LIKE 'TABLE_%E'")
      .returns(
        "TABLE_NAME=PARTITIONS; COLUMN_NAME=TABLE_NAME\n" +
        "TABLE_NAME=TABLES; COLUMN_NAME=TABLE_NAME\n" +
        "TABLE_NAME=TABLES; COLUMN_NAME=TABLE_TYPE\n" +
        "TABLE_NAME=TABLES; COLUMN_NAME=TABLE_SOURCE\n" +
        "TABLE_NAME=VIEWS; COLUMN_NAME=TABLE_NAME\n"
      );
  }

  @Test
  public void testSimilarNotSimilar() throws Exception{
    withNoDefaultSchema()
      .sql("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.`TABLES` "+
        "WHERE TABLE_NAME SIMILAR TO '%(H|I)E%' AND TABLE_NAME NOT SIMILAR TO 'C%' ORDER BY TABLE_NAME")
      .returns(
        "TABLE_NAME=SCHEMATA\n" +
        "TABLE_NAME=VIEWS\n"
      );
  }


  @Test
  public void testIntegerLiteral() throws Exception{
    withNoDefaultSchema()
      .sql("select substring('asd' from 1 for 2) from INFORMATION_SCHEMA.`TABLES` limit 1")
      .returns("EXPR$0=as\n");
  }

  @Test
  public void testNullOpForNullableType() throws Exception{
    withNoDefaultSchema()
        .sql("SELECT * FROM cp.`test_null_op.json` WHERE intType IS NULL AND varCharType IS NOT NULL")
        .returns("intType=null; varCharType=val2");
  }

  @Test
  public void testNullOpForNonNullableType() throws Exception{
    // output of (intType IS NULL) is a non-nullable type
     withNoDefaultSchema()
        .sql("SELECT * FROM cp.`test_null_op.json` "+
            "WHERE (intType IS NULL) IS NULL AND (varCharType IS NOT NULL) IS NOT NULL")
        .returns("");
  }

  @Test
  public void testTrueOpForNullableType() throws Exception{
     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE booleanType IS TRUE")
        .returns("data=set to true");

     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE booleanType IS FALSE")
        .returns("data=set to false");

     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE booleanType IS NOT TRUE")
        .returns(
            "data=set to false\n" +
            "data=not set"
        );

     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE booleanType IS NOT FALSE")
        .returns(
            "data=set to true\n" +
            "data=not set"
        );
  }

  @Test
  public void testTrueOpForNonNullableType() throws Exception{
    // Output of IS TRUE (and others) is a Non-nullable type
     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE (booleanType IS TRUE) IS TRUE")
        .returns("data=set to true");

     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE (booleanType IS FALSE) IS FALSE")
        .returns(
            "data=set to true\n" +
            "data=not set"
        );

     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE (booleanType IS NOT TRUE) IS NOT TRUE")
        .returns("data=set to true");

     withNoDefaultSchema()
        .sql("SELECT data FROM cp.`test_true_false_op.json` WHERE (booleanType IS NOT FALSE) IS NOT FALSE")
        .returns(
            "data=set to true\n" +
            "data=not set"
        );
  }

  @Test
  public void testDateTimeAccessors() throws Exception{
    withNoDefaultSchema().withConnection(new Function<Connection, Void>() {
      public Void apply(Connection connection) {
        try {
          final Statement statement = connection.createStatement();

          // show tables on view
          final ResultSet resultSet = statement.executeQuery(
              "select date '2008-2-23', time '12:23:34', timestamp '2008-2-23 12:23:34.456', " +
              "interval '1' year, interval '2' day, " +
              "date_add(date '2008-2-23', interval '1 10:20:30' day to second), " +
              "date_add(date '2010-2-23', 1) " +
              "from cp.`employee.json` limit 1");

          resultSet.next();
          final java.sql.Date date = resultSet.getDate(1);
          final java.sql.Time time = resultSet.getTime(2);
          final java.sql.Timestamp ts = resultSet.getTimestamp(3);
          final String intervalYear = resultSet.getString(4);
          final String intervalDay  = resultSet.getString(5);
          final java.sql.Timestamp ts1 = resultSet.getTimestamp(6);
          final java.sql.Date date1 = resultSet.getDate(7);

          final java.sql.Timestamp result = java.sql.Timestamp.valueOf("2008-2-24 10:20:30");
          final java.sql.Date result1 = java.sql.Date.valueOf("2010-2-24");
          assertEquals(ts1, result);
          assertEquals(date1, result1);

          // TODO:  Purge nextUntilEnd(...) and calls when remaining fragment
          // race conditions are fixed (not just DRILL-2245 fixes).
          // nextUntilEnd(resultSet);
          statement.close();
          return null;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Test
  public void testVerifyMetadata() throws Exception{
     withNoDefaultSchema().withConnection(new Function<Connection, Void>() {
      public Void apply(Connection connection) {
        try {
          final Statement statement = connection.createStatement();

          // show files
          final ResultSet resultSet = statement.executeQuery(
              "select timestamp '2008-2-23 12:23:23', date '2001-01-01' from cp.`employee.json` limit 1");

          assertEquals( Types.TIMESTAMP, resultSet.getMetaData().getColumnType(1) );
          assertEquals( Types.DATE, resultSet.getMetaData().getColumnType(2) );

          logger.debug(JdbcTestBase.toString(resultSet));
          resultSet.close();
          statement.close();
          return null;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @Test
  public void testCaseWithNoElse() throws Exception {
     withNoDefaultSchema()
        .sql("SELECT employee_id, CASE WHEN employee_id < 100 THEN first_name END from cp.`employee.json` " +
            "WHERE employee_id = 99 OR employee_id = 100")
        .returns(
            "employee_id=99; EXPR$1=Elizabeth\n" +
            "employee_id=100; EXPR$1=null\n"
        );
  }

  @Test
  public void testCaseWithElse() throws Exception {
     withNoDefaultSchema()
        .sql("SELECT employee_id, CASE WHEN employee_id < 100 THEN first_name ELSE 'Test' END from cp.`employee.json` " +
            "WHERE employee_id = 99 OR employee_id = 100")
        .returns(
            "employee_id=99; EXPR$1=Elizabeth\n" +
            "employee_id=100; EXPR$1=Test"
        );
  }

  @Test
  public void testCaseWith2ThensAndNoElse() throws Exception {
     withNoDefaultSchema()
        .sql("SELECT employee_id, CASE WHEN employee_id < 100 THEN first_name WHEN employee_id = 100 THEN last_name END " +
            "from cp.`employee.json` " +
            "WHERE employee_id = 99 OR employee_id = 100 OR employee_id = 101")
        .returns(
            "employee_id=99; EXPR$1=Elizabeth\n" +
            "employee_id=100; EXPR$1=Hunt\n" +
            "employee_id=101; EXPR$1=null"
        );
  }

  @Test
  public void testCaseWith2ThensAndElse() throws Exception {
     withNoDefaultSchema()
        .sql("SELECT employee_id, CASE WHEN employee_id < 100 THEN first_name WHEN employee_id = 100 THEN last_name ELSE 'Test' END " +
            "from cp.`employee.json` " +
            "WHERE employee_id = 99 OR employee_id = 100 OR employee_id = 101")
        .returns(
            "employee_id=99; EXPR$1=Elizabeth\n" +
            "employee_id=100; EXPR$1=Hunt\n" +
            "employee_id=101; EXPR$1=Test\n"
        );
  }

  @Test
  public void testAggWithDrillFunc() throws Exception {
     withNoDefaultSchema()
        .sql("SELECT extract(year from max(to_timestamp(hire_date, 'yyyy-MM-dd HH:mm:SS.SSS' ))) as MAX_YEAR " +
            "from cp.`employee.json` ")
        .returns(
            "MAX_YEAR=1998\n"
        );
  }

  @Test
  public void testLeftRightReplace() throws Exception {
     withNoDefaultSchema()
        .sql("SELECT `left`('abcdef', 2) as LEFT_STR, `right`('abcdef', 2) as RIGHT_STR, `replace`('abcdef', 'ab', 'zz') as REPLACE_STR " +
            "from cp.`employee.json` limit 1")
        .returns(
            "LEFT_STR=ab; " +
                "RIGHT_STR=ef; " +
                "REPLACE_STR=zzcdef\n"
        );
  }

  @Test
  public void testLengthUTF8VarCharInput() throws Exception {
     withNoDefaultSchema()
        .sql("select length('Sheri', 'UTF8') as L_UTF8 " +
            "from cp.`employee.json` where employee_id = 1")
        .returns(
            "L_UTF8=5\n"
       );
  }

  @Test
  public void testTimeIntervalAddOverflow() throws Exception {
     withNoDefaultSchema()
        .sql("select extract(hour from (interval '10 20' day to hour + time '10:00:00')) as TIME_INT_ADD " +
            "from cp.`employee.json` where employee_id = 1")
        .returns(
            "TIME_INT_ADD=6\n"
        );
  }

  @Test // DRILL-1051
  public void testOldDateTimeJulianCalendar() throws Exception {
     // Should work with any timezone
     withNoDefaultSchema()
        .sql("select cast(to_timestamp('1581-12-01 23:32:01', 'yyyy-MM-dd HH:mm:ss') as date) as `DATE`, " +
            "to_timestamp('1581-12-01 23:32:01', 'yyyy-MM-dd HH:mm:ss') as `TIMESTAMP`, " +
            "cast(to_timestamp('1581-12-01 23:32:01', 'yyyy-MM-dd HH:mm:ss') as time) as `TIME` " +
            "from (VALUES(1))")
        .returns("DATE=1581-12-01; TIMESTAMP=1581-12-01 23:32:01.0; TIME=23:32:01");
  }

  @Test // DRILL-1051
  public void testOldDateTimeLocalMeanTime() throws Exception {
     // Should work with any timezone
     withNoDefaultSchema()
        .sql("select cast(to_timestamp('1883-11-16 01:32:01', 'yyyy-MM-dd HH:mm:ss') as date) as `DATE`, " +
            "to_timestamp('1883-11-16 01:32:01', 'yyyy-MM-dd HH:mm:ss') as `TIMESTAMP`, " +
            "cast(to_timestamp('1883-11-16 01:32:01', 'yyyy-MM-dd HH:mm:ss') as time) as `TIME` " +
            "from (VALUES(1))")
        .returns("DATE=1883-11-16; TIMESTAMP=1883-11-16 01:32:01.0; TIME=01:32:01");
  }

  @Test // DRILL-5792
  public void testConvertFromInEmptyInputSql() throws Exception {
     withNoDefaultSchema()
        .sql("SELECT CONVERT_FROM(columns[1], 'JSON') as col1 from cp.`empty.csv`")
        .returns("");
  }

  @Test
  public void testResultSetIsNotReturnedSet() throws Exception {
    try (Connection conn = connect();
         Statement s = conn.createStatement()) {

      s.execute(String.format("SET `%s` = false", ExecConstants.RETURN_RESULT_SET_FOR_DDL));

      // Set any option
      s.execute(String.format("SET `%s` = 'json'", ExecConstants.OUTPUT_FORMAT_OPTION));
      assertNull("No result", s.getResultSet());
    }
  }

  @Test
  public void testResultSetIsNotReturnedCTAS() throws Exception {
    String tableName = "dfs.tmp.`ctas`";

    try (Connection conn = connect();
         Statement s = conn.createStatement()) {
      s.execute(String.format("SET `%s` = false", ExecConstants.RETURN_RESULT_SET_FOR_DDL));

      s.execute(String.format("CREATE TABLE %s AS SELECT * FROM cp.`employee.json`", tableName));
      assertNull("No result", s.getResultSet());
    } finally {
      execute("DROP TABLE IF EXISTS %s", tableName);
    }
  }

  @Test
  public void testResultSetIsNotReturnedCreateView() throws Exception {
    String viewName = "dfs.tmp.`cv`";

    try (Connection conn = connect();
         Statement s = conn.createStatement()) {
      s.execute(String.format("SET `%s` = false", ExecConstants.RETURN_RESULT_SET_FOR_DDL));

      s.execute(String.format("CREATE VIEW %s AS SELECT * FROM cp.`employee.json`", viewName));
      assertNull("No result", s.getResultSet());
    } finally {
      execute("DROP VIEW IF EXISTS %s", viewName);
    }
  }

  @Test
  public void testResultSetIsNotReturnedDropTable() throws Exception {
    String tableName = "dfs.tmp.`dt`";

    try (Connection conn = connect();
         Statement s = conn.createStatement()) {
      s.execute(String.format("SET `%s` = false", ExecConstants.RETURN_RESULT_SET_FOR_DDL));

      s.execute(String.format("CREATE TABLE %s AS SELECT * FROM cp.`employee.json`", tableName));

      s.execute(String.format("DROP TABLE %s", tableName));
      assertNull("No result", s.getResultSet());
    }
  }

  @Test
  public void testResultSetIsNotReturnedDropView() throws Exception {
    String viewName = "dfs.tmp.`dv`";

    try (Connection conn = connect();
         Statement stmt = conn.createStatement()) {
      stmt.execute(String.format("SET `%s` = false", ExecConstants.RETURN_RESULT_SET_FOR_DDL));

      stmt.execute(String.format("CREATE VIEW %s AS SELECT * FROM cp.`employee.json`", viewName));

      stmt.execute(String.format("DROP VIEW %s", viewName));
      assertNull("No result", stmt.getResultSet());
    }
  }

  @Test
  public void testResultSetIsNotReturnedUse() throws Exception {
    try (Connection conn = connect();
         Statement s = conn.createStatement()) {
      s.execute(String.format("SET `%s` = false", ExecConstants.RETURN_RESULT_SET_FOR_DDL));

      s.execute("USE dfs.tmp");
      assertNull("No result", s.getResultSet());
    }
  }

  @Test
  public void testResultSetIsNotReturnedRefreshMetadata() throws Exception {
    String tableName = "dfs.tmp.`rm`";

    try (Connection conn = connect();
         Statement s = conn.createStatement()) {
      s.execute(String.format("SET `%s` = false", ExecConstants.RETURN_RESULT_SET_FOR_DDL));

      s.execute(String.format("CREATE TABLE %s AS SELECT * FROM cp.`employee.json`", tableName));

      s.execute(String.format("REFRESH TABLE METADATA %s", tableName));
      assertNull("No result", s.getResultSet());
    }
  }

  private static void execute(String sql, Object... params) throws Exception {
    try (Connection conn = connect();
         Statement s = conn.createStatement()) {
      s.execute(String.format(sql, params));
    }
  }
}
