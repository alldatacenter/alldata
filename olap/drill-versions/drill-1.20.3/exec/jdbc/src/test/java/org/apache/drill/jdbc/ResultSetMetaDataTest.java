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
package org.apache.drill.jdbc;

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test class for Drill's java.sql.ResultSetMetaData implementation.
 * <p>
 *   Based on JDBC 4.1 (Java 7).
 * </p>
 */
@Category(JdbcTest.class)
public class ResultSetMetaDataTest extends JdbcTestBase {
  private static final String VIEW_NAME =
      ResultSetMetaDataTest.class.getSimpleName() + "_View";

  /** The one shared JDBC connection to Drill. */
  private static Connection connection;

  // Result set with test columns of various types.  Is positioned at first row
  // for, and must not be modified by, test methods.
  private static ResultSet viewRow;

  // Metadata for result set.
  private static ResultSetMetaData rowMetadata;

  //////////
  // For columns in temporary test view (types accessible via casting):

  // (Dynamic to make it simpler to add or remove columns.)
  private static int columnCount;

  private static int ordOptBOOLEAN;
  private static int ordReqBOOLEAN;

  private static int ordReqSMALLINT;
  private static int ordReqINTEGER;
  private static int ordReqBIGINT;

  private static int ordReqREAL;
  private static int ordReqFLOAT;
  private static int ordReqDOUBLE;

  private static int ordReqDECIMAL_5_3;
  // No NUMERIC while Drill just maps it to DECIMAL.

  private static int ordReqVARCHAR_10;
  private static int ordOptVARCHAR;
  private static int ordReqCHAR_5;
  // No NCHAR, etc., in Drill (?).
  private static int ordOptVARBINARY_16;
  private static int ordOptBINARY_1048576;

  private static int ordReqDATE;
  private static int ordReqTIME_2;
  private static int ordOptTIME_7;
  private static int ordReqTIMESTAMP_4;
  // No "... WITH TIME ZONE" in Drill.

  private static int ordReqINTERVAL_Y;
  private static int ordReqINTERVAL_3Y_Mo;
  private static int ordReqINTERVAL_10Y_Mo;
  private static int ordReqINTERVAL_Mo;
  private static int ordReqINTERVAL_D;
  private static int ordReqINTERVAL_4D_H;
  private static int ordReqINTERVAL_3D_Mi;
  private static int ordReqINTERVAL_2D_S5;
  private static int ordReqINTERVAL_H;
  private static int ordReqINTERVAL_1H_Mi;
  private static int ordReqINTERVAL_3H_S1;
  private static int ordReqINTERVAL_Mi;
  private static int ordReqINTERVAL_5Mi_S;
  private static int ordReqINTERVAL_S;
  private static int ordReqINTERVAL_3S;
  private static int ordReqINTERVAL_3S1;


  @BeforeClass
  public static void setUpConnectionAndMetadataToCheck() throws Exception {

    // Get JDBC connection to Drill:
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = connect("jdbc:drill:zk=local");
    final Statement stmt = connection.createStatement();

    ResultSet util;

    // Create temporary test-columns view:
    util = stmt.executeQuery( "USE `" + DFS_TMP_SCHEMA + "`" );
    assertTrue( util.next() );
    assertTrue( "Error setting schema for test: " + util.getString( 2 ),
                util.getBoolean( 1 ) );

    columnCount = 0;
    final StringBuilder buf = new StringBuilder();

    buf.append( "CREATE OR REPLACE VIEW `" + VIEW_NAME + "` AS SELECT " );

    buf.append( "\n CAST( NULL    AS BOOLEAN      ) AS mdrOptBOOLEAN, " );
    ordOptBOOLEAN = ++columnCount;
    buf.append( "\n TRUE                            AS mdrReqBOOLEAN, " );
    ordReqBOOLEAN = ++columnCount;

    //buf.append( "\n CAST(   15    AS SMALLINT     ) AS mdrOptSMALLINT, " );
    //ordOptSMALLINT = ++columnCount;
    buf.append( "\n CAST(    2    AS INTEGER      ) AS mdrOptINTEGER, " );
    ordReqINTEGER = ++columnCount;
    buf.append( "\n CAST( 15      AS BIGINT       ) AS mdrReqBIGINT, " );
    ordReqBIGINT = ++columnCount;


    // TODO(DRILL-2683): unignore when REAL is implemented:
    //buf.append( "\n CAST(  3.1    AS REAL         ) AS mdrReqREAL, " );
    //ordReqREAL = ++columnCount;
    buf.append( "\n CAST(  3.2    AS FLOAT        ) AS mdrReqFLOAT, " );
    ordReqFLOAT = ++columnCount;
    buf.append( "\n CAST(  3.3    AS DOUBLE       ) AS mdrReqDOUBLE, " );
    ordReqDOUBLE = ++columnCount;

    buf.append( "\n CAST(  4.4    AS DECIMAL(5,3) ) AS mdrReqDECIMAL_5_3, " );
    ordReqDECIMAL_5_3 = ++columnCount;

    buf.append( "\n CAST( 'Hi'    AS VARCHAR(10)  ) AS mdrReqVARCHAR_10, " );
    ordReqVARCHAR_10 = ++columnCount;
    buf.append( "\n CAST( NULL    AS VARCHAR      ) AS mdrOptVARCHAR, " );
    ordOptVARCHAR = ++columnCount;
    buf.append( "\n CAST( '55'    AS CHAR(5)      ) AS mdrReqCHAR_5, " );
    ordReqCHAR_5 = ++columnCount;

    // TODO(DRILL-3368): unignore when VARBINARY is implemented enough:
    //buf.append( "\n CAST( NULL    AS VARBINARY(16)      ) AS mdrOptVARBINARY_16," );
    //ordOptVARBINARY_16 = ++columnCount;
    // TODO(DRILL-3368): unignore when BINARY is implemented enough:
    //buf.append( "\n CAST( NULL    AS BINARY(1048576)    ) AS mdrOptBINARY_1048576, " );
    //ordOptBINARY_1048576 = ++columnCount;

    buf.append( "\n       DATE '2015-01-01'            AS mdrReqDATE, " );
    ordReqDATE = ++columnCount;
    buf.append( "\n CAST( TIME '23:59:59.123' AS TIME(2) ) AS mdrReqTIME_2, " );
    ordReqTIME_2 = ++columnCount;
    buf.append( "\n CAST( NULL                AS TIME(7) ) AS mdrOptTIME_7, " );
    ordOptTIME_7 = ++columnCount;
    buf.append( "\n CAST( TIMESTAMP '2015-01-01 23:59:59.12345'"
                +                  " AS TIMESTAMP(4) ) AS mdrReqTIMESTAMP_4, " );
    ordReqTIMESTAMP_4 = ++columnCount;

    buf.append( "\n INTERVAL '1'     YEAR              AS mdrReqINTERVAL_Y, " );
    ordReqINTERVAL_Y = ++columnCount;
    buf.append( "\n INTERVAL '1-2'   YEAR(3) TO MONTH  AS mdrReqINTERVAL_3Y_Mo, " );
    ordReqINTERVAL_3Y_Mo = ++columnCount;
    buf.append( "\n INTERVAL '1-2'   YEAR(10) TO MONTH AS mdrReqINTERVAL_10Y_Mo, " );
    ordReqINTERVAL_10Y_Mo = ++columnCount;
    buf.append( "\n INTERVAL '-2'    MONTH             AS mdrReqINTERVAL_Mo, " );
    ordReqINTERVAL_Mo = ++columnCount;
    buf.append( "\n INTERVAL '3'     DAY               AS mdrReqINTERVAL_D, " );
    ordReqINTERVAL_D = ++columnCount;
    buf.append( "\n INTERVAL '3 4'   DAY(4) TO HOUR    AS mdrReqINTERVAL_4D_H, " );
    ordReqINTERVAL_4D_H = ++columnCount;
    buf.append( "\n INTERVAL '3 4:5' DAY(3) TO MINUTE  AS mdrReqINTERVAL_3D_Mi, " );
    ordReqINTERVAL_3D_Mi = ++columnCount;
    buf.append( "\n INTERVAL '3 4:5:6' DAY(2) TO SECOND(5) AS mdrReqINTERVAL_2D_S5, " );
    ordReqINTERVAL_2D_S5 = ++columnCount;
    buf.append( "\n INTERVAL '4'     HOUR              AS mdrReqINTERVAL_H, " );
    ordReqINTERVAL_H = ++columnCount;
    buf.append( "\n INTERVAL '4:5'   HOUR(1) TO MINUTE AS mdrReqINTERVAL_1H_Mi, " );
    ordReqINTERVAL_1H_Mi = ++columnCount;
    buf.append( "\n INTERVAL '4:5:6' HOUR(3) TO SECOND(1) AS mdrReqINTERVAL_3H_S1, " );
    ordReqINTERVAL_3H_S1 = ++columnCount;
    buf.append( "\n INTERVAL '5'     MINUTE            AS mdrReqINTERVAL_Mi, " );
    ordReqINTERVAL_Mi = ++columnCount;
    buf.append( "\n INTERVAL '5:6'   MINUTE(5) TO SECOND AS mdrReqINTERVAL_5Mi_S, " );
    ordReqINTERVAL_5Mi_S = ++columnCount;
    buf.append( "\n INTERVAL '6'     SECOND          AS mdrReqINTERVAL_S, " );
    ordReqINTERVAL_S = ++columnCount;
    buf.append( "\n INTERVAL '6'     SECOND(3)       AS mdrReqINTERVAL_3S, " );
    ordReqINTERVAL_3S = ++columnCount;
    buf.append( "\n INTERVAL '6'     SECOND(3, 1)    AS mdrReqINTERVAL_3S1, " );
    ordReqINTERVAL_3S1 = ++columnCount;

    buf.append( "\n ''" );
    ++columnCount;
    buf.append( "\nFROM INFORMATION_SCHEMA.COLUMNS LIMIT 1 " );

    final String query = buf.toString();
    util = stmt.executeQuery( query );
    assertTrue( util.next() );
    assertTrue( "Error creating temporary test-columns view " + VIEW_NAME + ": "
                + util.getString( 2 ), util.getBoolean( 1 ) );

    viewRow = stmt.executeQuery( "SELECT * FROM " + VIEW_NAME + " LIMIT 1 " );
    viewRow.next();

    rowMetadata = viewRow.getMetaData();
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    final ResultSet util =
        connection.createStatement().executeQuery( "DROP VIEW " + VIEW_NAME + "" );
    assertTrue( util.next() );
    assertTrue( "Error dropping temporary test-columns view " + VIEW_NAME + ": "
                + util.getString( 2 ), util.getBoolean( 1 ) );
    connection.close();
  }


  //////////////////////////////////////////////////////////////////////
  // Tests:

  ////////////////////////////////////////////////////////////
  // getColumnCount(...):
  // JDBC: "Returns the number of columns in this ResultSet object."

  @Test
  public void test_getColumnCount() throws SQLException {
    assertThat( "column count",
                rowMetadata.getColumnCount(), equalTo( columnCount ) );
  }


  ////////////////////////////////////////////////////////////
  // isAutoIncrement(...):
  // JDBC: "Indicates whether the designated column is automatically numbered."

  @Test
  public void test_isAutoIncrement_returnsFalse() throws SQLException {
    assertThat( rowMetadata.isAutoIncrement( ordOptBOOLEAN ), equalTo( false ) );
  }


  ////////////////////////////////////////////////////////////
  // isCaseSensitive(...):
  // JDBC: "Indicates whether a column's case matters."
  // (Presumably that refers to the column's name, not values.)
  // Matters for what (for which operations)?

  @Test
  public void test_isCaseSensitive_nameThisNonSpecific() throws SQLException {
    assertThat( rowMetadata.isCaseSensitive( ordOptBOOLEAN ), equalTo( false ) );
  }


  ////////////////////////////////////////////////////////////
  // isSearchable(...):
  // JDBC: "Indicates whether the designated column can be used in a where
  //   clause."
  // (Is there any reason a column couldn't be used in a WHERE clause?)

  @Test
  public void test_isSearchable_returnsTrue() throws SQLException {
    assertThat( rowMetadata.isSearchable( ordOptBOOLEAN ), equalTo( true ) );
  }


  ////////////////////////////////////////////////////////////
  // isCurrency(...):
  // JDBC: "Indicates whether the designated column is a cash value."

  @Test
  public void test_isCurrency_returnsFalse() throws SQLException {
    assertThat( rowMetadata.isCurrency( ordOptBOOLEAN ), equalTo( false ) );
  }

  ////////////////////////////////////////////////////////////
  // isNullable(...):
  // JDBC: "Indicates the nullability of values in the designated column."

  @Test
  public void test_isNullable_forNullable() throws SQLException {
    assertThat( rowMetadata.isNullable( ordOptBOOLEAN ),
                equalTo( ResultSetMetaData.columnNullable) );
  }

  @Test
  public void test_isNullable_forRequired() throws SQLException {
    assertThat( rowMetadata.isNullable( ordReqINTEGER ),
                equalTo( ResultSetMetaData.columnNoNulls) );
  }


  ////////////////////////////////////////////////////////////
  // isSigned(...):
  // JDBC: "Indicates whether values in the designated column are signed numbers."
  // (Does "signed numbers" include intervals (which are signed)?

  @Test
  public void test_isSigned_forBOOLEAN() throws SQLException {
    assertThat( rowMetadata.isSigned( ordOptBOOLEAN ), equalTo( false ) );
  }

  @Test
  public void test_isSigned_forINTEGER() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqINTEGER ), equalTo( true ) );
  }

  @Test
  public void test_isSigned_forDOUBLE() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqDOUBLE ), equalTo( true ) );
  }

  @Test
  public void test_isSigned_forDECIMAL_5_3() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqDECIMAL_5_3 ), equalTo( true ) );
  }

  @Test
  public void test_isSigned_forVARCHAR() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqVARCHAR_10 ), equalTo( false ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3368): unignore when VARBINARY is implemented enough" )
  public void test_isSigned_forBINARY_1048576() throws SQLException {
    assertThat( rowMetadata.isSigned( ordOptBINARY_1048576 ), equalTo( false ) );
  }

  @Test
  public void test_isSigned_forDate() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqDATE ), equalTo( false ) );
  }

  @Test
  public void test_isSigned_forTIME_2() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqTIME_2 ), equalTo( false ) );
  }

  @Test
  public void test_isSigned_forTIMESTAMP_4() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqTIMESTAMP_4 ), equalTo( false ) );
  }

  @Test
  public void test_isSigned_forINTERVAL_Y() throws SQLException {
    assertThat( rowMetadata.isSigned( ordReqINTERVAL_Y ), equalTo( true ) );
  }

  // TODO(DRILL-3253):  Do more types when we have all-types test storage plugin.


  ////////////////////////////////////////////////////////////
  // getColumnDisplaySize(...):
  // JDBC: "Indicates the designated column's normal maximum width in characters.
  //   ... the normal maximum number of characters allowed as the width of the
  //       designated column"
  // (What exactly is the "normal maximum" number of characters?)

  @Test
  public void test_getColumnDisplaySize_forBOOLEAN() throws SQLException {
    assertThat( rowMetadata.getColumnDisplaySize( ordOptBOOLEAN ),
                equalTo( 1 ) );
  }

  // TODO(DRILL-3355):  Do more types when metadata is available.
  // TODO(DRILL-3253):  Do more types when we have all-types test storage plugin.


  ////////////////////////////////////////////////////////////
  // getColumnLabel(...):
  // JDBC: "Gets the designated column's suggested title for use in printouts
  //   and displays. The suggested title is usually specified by the SQL
  //   AS clause.  If a SQL AS is not specified, the value returned from
  //   getColumnLabel will be the same as the value returned by the
  //   getColumnName method."

  @Test
  public void test_getColumnLabel_getsName() throws SQLException {
    assertThat( rowMetadata.getColumnLabel( ordOptBOOLEAN ),
                equalTo( "mdrOptBOOLEAN" ) );
  }


  ////////////////////////////////////////////////////////////
  // getColumnName(...):
  // JDBC: "Get the designated column's name."

  @Test
  public void test_getColumnName_getsName() throws SQLException {
    assertThat( rowMetadata.getColumnName( ordOptBOOLEAN ),
                equalTo( "mdrOptBOOLEAN" ) );
  }


  ////////////////////////////////////////////////////////////
  // getSchemaName(...):
  // JDBC: "Get the designated column's table's schema. ... schema name
  //   or "" if not applicable"
  // Note: Schema _name_, not schema, of course.
  // (Are result-set tables in a schema?)

  @Test
  public void test_getSchemaName_forViewGetsName() throws SQLException {
    assertThat( rowMetadata.getSchemaName( ordOptBOOLEAN ),
                anyOf( equalTo( DFS_TMP_SCHEMA ),
                       equalTo( "" ) ) );
  }


  ////////////////////////////////////////////////////////////
  // getPrecision(...):
  // JDBC: "Get the designated column's specified column size.
  //   For numeric data, this is the maximum precision.
  //   For character data, this is the length in characters.
  //   For datetime datatypes, this is the length in characters of the String
  //     representation (assuming the maximum allowed precision of the
  //     fractional seconds component).
  //   For binary data, this is the length in bytes.
  //   For the ROWID datatype, this is the length in bytes.
  //   0 is returned for data types where the column size is not applicable."
  // TODO(DRILL-3355):  Resolve:
  // - Confirm:  This seems to be the same as getColumns's COLUMN_SIZE.
  // - Is numeric "maximum precision" in bits, in digits, or per some radix
  //   specified somewhere?
  // - For which unmentioned types is column size applicable or not applicable?
  //   E.g., what about interval types?

  @Test
  public void test_getPrecision_forBOOLEAN() throws SQLException {
    assertThat( rowMetadata.getPrecision( ordOptBOOLEAN ), equalTo( 0 ) );
  }

  @Ignore( "TODO(DRILL-3355): unignore when getPrecision(...) implemented" )
  @Test
  public void test_getPrecision_forINTEGER() throws SQLException {
    // Is it actual bits?:
    assertThat( rowMetadata.getPrecision( ordReqINTEGER ), equalTo( 32 ) );
    // Is it number of possible decimal digits?
    assertThat( rowMetadata.getPrecision( ordReqINTEGER ), equalTo( 10 ) );
    // Is it minimum guaranteed decimal digits?
    assertThat( rowMetadata.getPrecision( ordReqINTEGER ), equalTo( 9 ) );
  }

  @Ignore( "TODO(DRILL-3355): unignore when getPrecision(...) implemented" )
  @Test
  public void test_getPrecision_forDOUBLE() throws SQLException {
    // Is it actual bits?:
    assertThat( rowMetadata.getPrecision( ordReqDOUBLE ), equalTo( 53 ) );
    // Is it number of possible decimal digits?
    assertThat( rowMetadata.getPrecision( ordReqINTEGER ), equalTo( 7 ) );
    // Is it minimum guaranteed decimal digits?
    assertThat( rowMetadata.getPrecision( ordReqDOUBLE ), equalTo( 6 ) );
  }

  @Ignore( "TODO(DRILL-3367): unignore when DECIMAL is no longer DOUBLE" )
  @Test
  public void test_getPrecision_forDECIMAL_5_3() throws SQLException {
    assertThat( rowMetadata.getPrecision( ordReqDECIMAL_5_3 ), equalTo( 5 ) );
  }

  // TODO(DRILL-3355):  Do more types when metadata is available.
  // - Copy in tests for DatabaseMetaData.getColumns(...)'s COLUMN_SIZE (since
  //   ResultSetMetaData.getPrecision(...) seems to be defined the same.
  // TODO(DRILL-3253):  Do more types when we have all-types test storage plugin.


  ////////////////////////////////////////////////////////////
  // getScale(...):
  // JDBC: "Gets the designated column's number of digits to right of the
  //   decimal point.  0 is returned for data types where the scale is not
  //   applicable."
  // (When exactly is scale not applicable?  What about for TIME or INTERVAL?)

  @Test
  public void test_getScale_forBOOLEAN() throws SQLException {
    assertThat( rowMetadata.getScale( ordOptBOOLEAN ), equalTo( 0 ) );
  }

  @Test
  public void test_getScale_forINTEGER() throws SQLException {
    assertThat( rowMetadata.getScale( ordReqINTEGER ), equalTo( 0 ) );
  }

  @Ignore( "TODO(DRILL-3367): unignore when DECIMAL is no longer DOUBLE" )
  @Test
  public void test_getScale_forDECIMAL_5_3() throws SQLException {
    assertThat( rowMetadata.getScale( ordReqDECIMAL_5_3 ), equalTo( 3 ) );
  }

  // TODO(DRILL-3355):  Do more types when metadata is available.
  // - especially TIME and INTERVAL cases.
  // TODO(DRILL-3253):  Do more types when we have all-types test storage plugin.


  ////////////////////////////////////////////////////////////
  // getTableName(...):
  // JDBC: "Gets the designated column's table name. ... table name or "" if
  //   not applicable"
  // (When exactly is this applicable or not applicable?)

  @Test
  public void test_getTableName_forViewGetsName() throws SQLException {
    assertThat( rowMetadata.getTableName( ordOptBOOLEAN ),
                anyOf( equalTo( VIEW_NAME ),
                       equalTo( "" ) ) );
  }


  ////////////////////////////////////////////////////////////
  // getCatalogName(...):
  // JDBC: "Gets the designated column's table's catalog name.  ... the name of
  //   the catalog for the table in which the given column appears or "" if not
  //    applicable"
  // (What if the result set is not directly from a base table?  Since Drill has
  // has only one catalog ("DRILL") should this return "DRILL" for everything,
  // or only for base tables?)

  @Test
  public void test_getCatalogName_getsCatalogName() throws SQLException {
    assertThat( rowMetadata.getCatalogName( ordOptBOOLEAN ),
                anyOf( equalTo( "DRILL" ), equalTo( "" ) ) );
  }


  ////////////////////////////////////////////////////////////
  // getColumnType(...):
  // JDBC: "Retrieves the designated column's SQL type.  ... SQL type from
  //   java.sql.Types"
  // NOTE:  JDBC representation of data type or data type family.

  @Test
  public void test_getColumnType_forBOOLEAN() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordOptBOOLEAN ),
                equalTo( Types.BOOLEAN ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_getColumnType_forSMALLINT() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqSMALLINT ),
                equalTo( Types.SMALLINT ) );
  }

  @Test
  public void test_getColumnType_forINTEGER() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqINTEGER ),
                equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_getColumnType_forBIGINT() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqBIGINT ),
                equalTo( Types.BIGINT ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_getColumnType_forREAL() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqREAL ),
                equalTo( Types.REAL ) );
  }

  @Test
  public void test_getColumnType_forFLOAT() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqFLOAT ),
                equalTo( Types.FLOAT ) );
  }

  @Test
  public void test_getColumnType_forDOUBLE() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqDOUBLE ),
                equalTo( Types.DOUBLE ) );
  }

  @Ignore( "TODO(DRILL-3367): unignore when DECIMAL is no longer DOUBLE" )
  @Test
  public void test_getColumnType_forDECIMAL_5_3() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqDECIMAL_5_3 ),
                equalTo( Types.DECIMAL ) );
  }

  @Test
  public void test_getColumnType_forVARCHAR_10() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqVARCHAR_10 ),
                equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_getColumnType_forVARCHAR() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordOptVARCHAR ),
                equalTo( Types.VARCHAR ) );
  }

  @Ignore( "TODO(DRILL-3369): unignore when CHAR is no longer VARCHAR" )
  @Test
  public void test_getColumnType_forCHAR_5() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqCHAR_5 ),
                equalTo( Types.CHAR ) );
  }

  @Ignore( "TODO(DRILL-3368): unignore when VARBINARY is implemented enough" )
  @Test
  public void test_getColumnType_forVARBINARY_16() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordOptVARBINARY_16 ),
                equalTo( Types.VARBINARY ) );
  }

  @Ignore( "TODO(DRILL-3368): unignore when BINARY is implemented enough" )
  @Test
  public void test_getColumnType_forBINARY_1048576CHECK() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordOptBINARY_1048576 ),
                equalTo( Types.VARBINARY ) );
  }

  @Test
  public void test_getColumnType_forDATE() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqDATE ),
                equalTo( Types.DATE ) );
  }

  @Test
  public void test_getColumnType_forTIME_2() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqTIME_2 ),
                equalTo( Types.TIME ) );
  }

  @Test
  public void test_getColumnType_forTIME_7() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordOptTIME_7 ),
                equalTo( Types.TIME ) );
  }

  @Test
  public void test_getColumnType_forTIMESTAMP_4() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqTIMESTAMP_4 ),
                equalTo( Types.TIMESTAMP ) );
  }

  @Test
  public void test_getColumnType_forINTERVAL_Y() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqINTERVAL_Y ),
                equalTo( Types.OTHER ) );
  }

  @Test
  public void test_getColumnType_forINTERVAL_H_S3() throws SQLException {
    assertThat( rowMetadata.getColumnType( ordReqINTERVAL_3H_S1 ),
                equalTo( Types.OTHER ) );
  }

  // TODO(DRILL-3253):  Do more types when we have all-types test storage plugin.


  ////////////////////////////////////////////////////////////
  // getColumnTypeName(...):
  // JDBC: "Retrieves the designated column's database-specific type name.
  //   ... type name used by the database.  If the column type is a user-defined
  //   type, then a fully-qualified type name is returned."
  // (Is this expected to match INFORMATION_SCHEMA.COLUMNS.TYPE_NAME?)

  @Test
  public void test_getColumnTypeName_forBOOLEAN() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordOptBOOLEAN ),
                equalTo( "BOOLEAN" ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_getColumnTypeName_forSMALLINT() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqSMALLINT ),
                equalTo( "SMALLINT" ) );
  }

  @Test
  public void test_getColumnTypeName_forINTEGER() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqINTEGER ),
                equalTo( "INTEGER" ) );
  }

  @Test
  public void test_getColumnTypeName_forBIGINT() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqBIGINT ),
                equalTo( "BIGINT" ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_getColumnTypeName_forREAL() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqREAL ),
                equalTo( "REAL" ) );
  }

  @Test
  public void test_getColumnTypeName_forFLOAT() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqFLOAT ),
                equalTo( "FLOAT" ) );
  }

  @Test
  public void test_getColumnTypeName_forDOUBLE() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqDOUBLE ),
                equalTo( "DOUBLE" ) );
  }

  @Ignore( "TODO(DRILL-3367): unignore when DECIMAL is no longer DOUBLE" )
  @Test
  public void test_getColumnTypeName_forDECIMAL_5_3() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqDECIMAL_5_3 ),
                equalTo( "DECIMAL" ) );
  }

  @Test
  public void test_getColumnTypeName_forVARCHAR() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordOptVARCHAR ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Ignore( "TODO(DRILL-3369): unignore when CHAR is no longer VARCHAR" )
  @Test
  public void test_getColumnTypeName_forCHAR() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqCHAR_5 ),
                equalTo( "CHARACTER" ) );
  }

  @Ignore( "TODO(DRILL-3368): unignore when VARBINARY is implemented enough" )
  @Test
  public void test_getColumnTypeName_forVARBINARY() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordOptVARBINARY_16 ),
                equalTo( "BINARY VARYING" ) );
  }

  @Ignore( "TODO(DRILL-3368): unignore when BINARY is implemented enough" )
  @Test
  public void test_getColumnTypeName_forBINARY() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordOptBINARY_1048576 ),
                equalTo( "BINARY" ) );
  }

  @Test
  public void test_getColumnTypeName_forDATE() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqDATE ),
                equalTo( "DATE" ) );
  }

  @Test
  public void test_getColumnTypeName_forTIME_2() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqTIME_2 ),
                equalTo( "TIME" ) );
  }

  @Test
  public void test_getColumnTypeName_forTIMESTAMP_4() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqTIMESTAMP_4 ),
                equalTo( "TIMESTAMP" ) );
  }

  @Test
  public void test_getColumnTypeName_forINTERVAL_Y() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqINTERVAL_Y ),
                equalTo( "INTERVAL YEAR TO MONTH" ) );
  }

  @Test
  public void test_getColumnTypeName_forINTERVAL_D() throws SQLException {
    assertThat( rowMetadata.getColumnTypeName( ordReqINTERVAL_4D_H ),
                equalTo( "INTERVAL DAY TO SECOND" ) );
  }

  // TODO(DRILL-3253):  Do more types when we have all-types test storage plugin.


  ////////////////////////////////////////////////////////////
  // isReadOnly(...):
  // JDBC: "Indicates whether the designated column is definitely not writable."
  // (Writable in what context?  By current user in current connection? Some
  // other context?)

  @Test
  public void test_isReadOnly_nameThisNonSpecific() throws SQLException {
    assertThat( rowMetadata.isReadOnly( ordOptBOOLEAN ), equalTo( true ) );
  }


  ////////////////////////////////////////////////////////////
  // isWritable(...):
  // JDBC: "Indicates whether it is possible for a write on the designated
  //   column to succeed."
  // (Possible in what context?  By current user in current connection?  Some
  // other context?

  @Test
  public void test_isWritable_nameThisNonSpecific() throws SQLException {
    assertThat( rowMetadata.isWritable( ordOptBOOLEAN ), equalTo( false ) );
  }


  ////////////////////////////////////////////////////////////
  // isDefinitelyWritable(...):
  // JDBC: "Indicates whether a write on the designated column will definitely
  //   succeed."
  // (Will succeed in what context?  By current user in current connection?
  // Some other context?)

  @Test
  public void test_isDefinitelyWritable_nameThisNonSpecific() throws SQLException {
    assertThat( rowMetadata.isDefinitelyWritable( ordOptBOOLEAN ),
                equalTo( false ) );
  }


  ////////////////////////////////////////////////////////////
  // getColumnClassName(...):
  // JDBC: "Returns the fully-qualified name of the Java class whose instances
  //   are manufactured if the method ResultSet.getObject is called to retrieve
  //   a value from the column.  ResultSet.getObject may return a subclass of
  //   the class returned by this method. ... the fully-qualified name of the
  //   class in the Java programming language that would be used by the method"

  // BOOLEAN:

  @Test
  public void test_getColumnClassName_forBOOLEAN_isBoolean() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordOptBOOLEAN ),
                equalTo( Boolean.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forBOOLEAN_matches() throws SQLException {
    assertThat(
        rowMetadata.getColumnClassName( ordReqBOOLEAN ),
        // (equalTo because Boolean is final)
        equalTo( viewRow.getObject( ordReqBOOLEAN ).getClass().getName() ) );
  }

  // SMALLINT:

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_getColumnClassName_forSMALLINT_isShort() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqSMALLINT ),
                equalTo( Short.class.getName() ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_getColumnClassName_forSMALLINT_matches() throws SQLException {
    assertThat(
        rowMetadata.getColumnClassName( ordReqSMALLINT ),
        // (equalTo because Short is final)
        equalTo( viewRow.getObject( ordReqSMALLINT ).getClass().getName() ) );
  }

  // INTEGER:

  @Test
  public void test_getColumnClassName_forINTEGER_isInteger() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqINTEGER ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forINTEGER_matches() throws SQLException {
    assertThat(
        rowMetadata.getColumnClassName( ordReqINTEGER ),
        // (equalTo because Integer is final)
        equalTo( viewRow.getObject( ordReqINTEGER ).getClass().getName() ) );
  }

  // BIGINT:

  @Test
  public void test_getColumnClassName_forBIGINT_isLong() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqBIGINT ),
                equalTo( Long.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forBIGINT_matches() throws SQLException {
    assertThat(
        rowMetadata.getColumnClassName( ordReqBIGINT ),
        // (equalTo because Long is final)
        equalTo( viewRow.getObject( ordReqBIGINT ).getClass().getName() ) );
  }

  // REAL:

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_getColumnClassName_forREAL_isFloat() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqREAL ),
                equalTo( Float.class.getName() ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_getColumnClassName_forREAL_matches() throws SQLException {
    assertThat(
        rowMetadata.getColumnClassName( ordReqREAL ),
        // (equalTo because Float is final)
        equalTo( viewRow.getObject( ordReqREAL ).getClass().getName() ) );
  }

  // FLOAT:

  @Test
  public void test_getColumnClassName_forFLOAT_isFloat() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqFLOAT ),
                anyOf( equalTo( Float.class.getName() ),
                       equalTo( Double.class.getName() ) ) );
  }

  @Test
  public void test_getColumnClassName_forFLOAT_matches() throws SQLException {
    assertThat(
        rowMetadata.getColumnClassName( ordReqFLOAT ),
        // (equalTo because Float is final)
        equalTo( viewRow.getObject( ordReqFLOAT ).getClass().getName() ) );
  }

  // DOUBLE:

  @Test
  public void test_getColumnClassName_forDOUBLE_isDouble() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqDOUBLE ),
                equalTo( Double.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forDOUBLE_matches() throws SQLException {
    assertThat(
        rowMetadata.getColumnClassName( ordReqDOUBLE ),
        // (equalTo because Double is final)
        equalTo( viewRow.getObject( ordReqDOUBLE ).getClass().getName() ) );
  }

  // DECIMAL_5_3:

  @Ignore( "TODO(DRILL-3367): unignore when DECIMAL is no longer DOUBLE" )
  @Test
  public void test_getColumnClassName_forDECIMAL_5_3_isBigDecimal() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqDECIMAL_5_3 ),
                equalTo( BigDecimal.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forDECIMAL_5_3_matches()
      throws SQLException, ClassNotFoundException {
    final Class<?> requiredClass =
        Class.forName( rowMetadata.getColumnClassName( ordReqDECIMAL_5_3 ) );
    final Class<?> actualClass = viewRow.getObject( ordReqDECIMAL_5_3 ).getClass();
    assertTrue( "actual class " + actualClass.getName()
                + " is not assignable to required class " + requiredClass,
                requiredClass.isAssignableFrom( actualClass ) );
  }

  // VARCHAR_10:

  @Test
  public void test_getColumnClassName_forVARCHAR_10_isString() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqVARCHAR_10 ),
                equalTo( String.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forVARCHAR_10_matches()
      throws SQLException, ClassNotFoundException {
    final Class<?> requiredClass =
        Class.forName( rowMetadata.getColumnClassName( ordReqVARCHAR_10 ) );
    final Class<?> actualClass = viewRow.getObject( ordReqVARCHAR_10 ).getClass();
    assertTrue( "actual class " + actualClass.getName()
                + " is not assignable to required class " + requiredClass,
                requiredClass.isAssignableFrom( actualClass ) );
  }

  // TODO(DRILL-3369):  Add test when CHAR is no longer VARCHAR:
  // CHAR_5:
  // TODO(DRILL-3368):  Add test when VARBINARY is implemented enough:
  // VARBINARY_16
  // TODO(DRILL-3368):  Add test when BINARY is implemented enough:
  // BINARY_1048576:

  // DATE:

  @Test
  public void test_getColumnClassName_forDATE_isDate() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqDATE ),
                equalTo( Date.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forDATE_matches()
      throws SQLException, ClassNotFoundException {
    final Class<?> requiredClass =
        Class.forName( rowMetadata.getColumnClassName( ordReqDATE ) );
     final Class<?> actualClass = viewRow.getObject( ordReqDATE ).getClass();
     assertTrue(
        "actual class " + actualClass.getName()
        + " is not assignable to required class " + requiredClass,
        requiredClass.isAssignableFrom( actualClass ) );
  }

  // TIME:

  @Test
  public void test_getColumnClassName_forTIME_2_isTime() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqTIME_2 ),
                equalTo( Time.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forTIME_2_matches()
      throws SQLException, ClassNotFoundException {
    final Class<?> requiredClass =
        Class.forName( rowMetadata.getColumnClassName( ordReqTIME_2 ) );
    final Class<?> actualClass = viewRow.getObject( ordReqTIME_2 ).getClass();
    assertTrue(
        "actual class " + actualClass.getName()
        + " is not assignable to required class " + requiredClass,
        requiredClass.isAssignableFrom( actualClass ) );
  }

  // TIME_7:

  // TIMESTAMP:

  @Test
  public void test_getColumnClassName_forTIMESTAMP_4_isDate() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqTIMESTAMP_4 ),
                equalTo( Timestamp.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forTIMESTAMP_4_matches()
      throws SQLException, ClassNotFoundException {
    final Class<?> requiredClass =
        Class.forName( rowMetadata.getColumnClassName( ordReqTIMESTAMP_4 ) );
    final Class<?> actualClass =
        viewRow.getObject( ordReqTIMESTAMP_4 ).getClass();
    assertTrue(
        "actual class " + actualClass.getName()
        + " is not assignable to required class " + requiredClass,
        requiredClass.isAssignableFrom( actualClass ) );
  }

  // No "... WITH TIME ZONE" in Drill.

  // INTERVAL_Y:

  // INTERVAL_3Y_Mo:

  // INTERVAL_10Y_Mo:

  @Test
  public void test_getColumnClassName_forINTERVAL_10Y_Mo_isJodaPeriod() throws SQLException {
    assertThat( rowMetadata.getColumnClassName( ordReqINTERVAL_10Y_Mo ),
                equalTo( org.joda.time.Period.class.getName() ) );
  }

  @Test
  public void test_getColumnClassName_forINTERVAL_10Y_Mo_matches()
      throws SQLException, ClassNotFoundException {
    final Class<?> requiredClass =
        Class.forName( rowMetadata.getColumnClassName( ordReqINTERVAL_10Y_Mo ) );
    final Class<?> actualClass =
        viewRow.getObject( ordReqINTERVAL_10Y_Mo ).getClass();
    assertTrue( "actual class " + actualClass.getName()
                + " is not assignable to required class " + requiredClass,
                requiredClass.isAssignableFrom( actualClass ) );
  }

  // TODO(DRILL-3253):  Do more types when we have all-types test storage plugin.


} // class DatabaseMetaGetColumnsDataTest
