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

import static java.sql.ResultSetMetaData.columnNoNulls;
import static java.sql.ResultSetMetaData.columnNullable;
import static java.sql.ResultSetMetaData.columnNullableUnknown;
import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

// NOTE: TestInformationSchemaColumns and DatabaseMetaDataGetColumnsTest
// have identical sections.  (Cross-maintain them for now; factor out later.)

// TODO:  MOVE notes to implementation (have this not (just) in test).

// TODO:  Determine for each numeric type whether its precision is reported in
//   decimal or binary (and NUM_PREC_RADIX is 10 or 2, respectively).
//   The SQL specification for INFORMATION_SCHEMA.COLUMNS seems to specify the
//   radix for each numeric type:
//   - 2 or 10 for SMALLINT, INTEGER, and BIGINT;
//   - only 10 for NUMERIC and DECIMAL; and
//   - only 2  for REAL, FLOAT, and DOUBLE PRECISION.
//   However, it is not clear what the JDBC API intends:
//   - It has NUM_PREC_RADIX, specifying a radix or 10 or 2, but doesn't specify
//     exactly what is applies to.  Apparently, it applies to COLUMN_SIZE abd
//     ResultMetaData.getPrecision() (which are defined in terms of maximum
//     precision for numeric types).
//   - Is has DECIMAL_DIGITS, which is <em>not</em> the number of decimal digits
//     of precision, but which it defines as the number of fractional digits--
//     without actually specifying that it's in decimal.

// TODO:  Review nullability (NULLABLE and IS_NULLABLE columns):
// - It's not clear what JDBC's requirements are.
//   - It does seem obvious that metadata should not contradictorily say that a
//   - column cannot contain nulls when the column currently does contain nulls.
//   - It's not clear whether metadata must say that a column cannot contains
//     nulls if JDBC specifies that the column always has a non-null value.
// - It's not clear why Drill reports that columns that will never contain nulls
//   can contain nulls.
// - It's not clear why Drill sets INFORMATION_SCHEMA.COLUMNS.IS_NULLABLE to
//   'NO' for some columns that contain only null (e.g., for
//   "CREATE VIEW x AS SELECT CAST(NULL AS ...) ..."


/**
 * Test class for Drill's java.sql.DatabaseMetaData.getColumns() implementation.
 * <p>
 *   Based on JDBC 4.1 (Java 7).
 * </p>
 */
@Category(JdbcTest.class)
public class DatabaseMetaDataGetColumnsTest extends JdbcTestBase {
  private static final String VIEW_NAME =
      DatabaseMetaDataGetColumnsTest.class.getSimpleName() + "_View";

  /** The one shared JDBC connection to Drill. */
  protected static Connection connection;

  /** Overall (connection-level) metadata. */
  protected static DatabaseMetaData dbMetadata;

  /** getColumns result metadata.  For checking columns themselves (not cell
   *  values or row order). */
  protected static ResultSetMetaData rowsMetadata;


  ////////////////////
  // Results from getColumns for test columns of various types.
  // Each ResultSet is positioned at first row for, and must not be modified by,
  // test methods.

  //////////
  // For columns in temporary test view (types accessible via casting):

  private static ResultSet mdrOptBOOLEAN;

  // TODO(DRILL-2470): re-enable TINYINT, SMALLINT, and REAL.
  private static ResultSet mdrReqTINYINT;
  private static ResultSet mdrOptSMALLINT;
  private static ResultSet mdrReqINTEGER;
  private static ResultSet mdrOptBIGINT;

  // TODO(DRILL-2470): re-enable TINYINT, SMALLINT, and REAL.
  private static ResultSet mdrOptREAL;
  private static ResultSet mdrOptFLOAT;
  private static ResultSet mdrReqDOUBLE;

  private static ResultSet mdrReqDECIMAL_5_3;
  // No NUMERIC while Drill just maps it to DECIMAL.

  private static ResultSet mdrReqVARCHAR_10;
  private static ResultSet mdrOptVARCHAR;
  private static ResultSet mdrReqCHAR_5;
  // No NCHAR, etc., in Drill (?).
  private static ResultSet mdrOptVARBINARY_16;
  private static ResultSet mdrOptBINARY_65536;

  private static ResultSet mdrReqDATE;
  private static ResultSet mdrReqTIME;
  private static ResultSet mdrOptTIME_7;
  private static ResultSet mdrOptTIMESTAMP;
  // No "... WITH TIME ZONE" in Drill.

  private static ResultSet mdrReqINTERVAL_Y;
  private static ResultSet mdrReqINTERVAL_3Y_Mo;
  private static ResultSet mdrReqINTERVAL_Mo;
  private static ResultSet mdrReqINTERVAL_D;
  private static ResultSet mdrReqINTERVAL_4D_H;
  private static ResultSet mdrReqINTERVAL_3D_Mi;
  private static ResultSet mdrReqINTERVAL_2D_S5;
  private static ResultSet mdrReqINTERVAL_H;
  private static ResultSet mdrReqINTERVAL_1H_Mi;
  private static ResultSet mdrReqINTERVAL_3H_S1;
  private static ResultSet mdrReqINTERVAL_Mi;
  private static ResultSet mdrReqINTERVAL_5Mi_S;
  private static ResultSet mdrReqINTERVAL_S;
  private static ResultSet mdrReqINTERVAL_3S;
  private static ResultSet mdrReqINTERVAL_3S1;

  // For columns in schema hive_test.default's infoschematest table:

  // listtype column:      VARCHAR(65535) ARRAY, non-null(?):
  private static ResultSet mdrReqARRAY;
  // maptype column:       (VARCHAR(65535), INTEGER) MAP, non-null(?):
  private static ResultSet mdrReqMAP;
  // structtype column:    STRUCT(INTEGER sint, BOOLEAN sboolean,
  //                              VARCHAR(65535) sstring), non-null(?):
  private static ResultSet mdrUnkSTRUCT;
  // uniontypetype column: OTHER (?), non=nullable(?):
  private static ResultSet mdrUnkUnion;


  private static ResultSet setUpRow( final String schemaName,
                                     final String tableOrViewName,
                                     final String columnName ) throws SQLException
  {
    assertNotNull( "dbMetadata is null; must be set before calling setUpRow(...)",
                   dbMetadata );
    final ResultSet testRow =
        dbMetadata.getColumns( "DRILL", schemaName, tableOrViewName, columnName );
    assertTrue( "Test setup error:  No row for column DRILL . `" + schemaName
                + "` . `" + tableOrViewName + "` . `" + columnName + "`",
                testRow.next() );
    return testRow;
  }

  @BeforeClass
  public static void setUpConnection() throws Exception {
    // Get JDBC connection to Drill:
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = connect();
    dbMetadata = connection.getMetaData();

    setUpMetadataToCheck();
  }

  protected static void setUpMetadataToCheck() throws Exception {
    final Statement stmt = connection.createStatement();

    ResultSet util;

    /* TODO(DRILL-3253)(start): Update this once we have test plugin supporting all needed types
    // Create Hive test data, only if not created already (speed optimization):
    util = stmt.executeQuery( "SELECT * FROM INFORMATION_SCHEMA.COLUMNS "
                              + "WHERE TABLE_SCHEMA = 'hive_test.default' "
                              + "  AND TABLE_NAME = 'infoschematest'" );

    int hiveTestColumnRowCount = 0;
    while ( util.next() ) {
      hiveTestColumnRowCount++;
    }
    if ( 0 == hiveTestColumnRowCount ) {
      // No Hive test data--create it.
      new HiveTestDataGenerator().generateTestData();
    } else if ( 17 == hiveTestColumnRowCount ) {
      // Hive data seems to exist already--skip recreating it.
    } else {
      fail("Expected 17 Hive test columns see " + hiveTestColumnRowCount + "."
            + "  Test code is out of date or Hive data is corrupted.");
    }
    TODO(DRILL-3253)(end) */

    // Create temporary test-columns view:
    util = stmt.executeQuery( "USE dfs.tmp" );
    assertTrue( util.next() );
    assertTrue( "Error setting schema for test: " + util.getString( 2 ),
                util.getBoolean( 1 ) );
    // TODO(DRILL-2470): Adjust when TINYINT is implemented:
    // TODO(DRILL-2470): Adjust when SMALLINT is implemented:
    // TODO(DRILL-2683): Adjust when REAL is implemented:
    util = stmt.executeQuery(
        ""
        +   "CREATE OR REPLACE VIEW " + VIEW_NAME + " AS SELECT "
        + "\n  CAST( NULL    AS BOOLEAN            ) AS mdrOptBOOLEAN,        "
        + "\n  "
        + "\n  CAST(    1    AS INT            ) AS mdrReqTINYINT,        "
        + "\n  CAST( NULL    AS INT           ) AS mdrOptSMALLINT,       "
        //+ "\n  CAST(    1    AS TINYINT            ) AS mdrReqTINYINT,        "
        //+ "\n  CAST( NULL    AS SMALLINT           ) AS mdrOptSMALLINT,       "
        + "\n  CAST(    2    AS INTEGER            ) AS mdrReqINTEGER,        "
        + "\n  CAST( NULL    AS BIGINT             ) AS mdrOptBIGINT,         "
        + "\n  "
        + "\n  CAST( NULL    AS FLOAT               ) AS mdrOptREAL,           "
        //+ "\n  CAST( NULL    AS REAL               ) AS mdrOptREAL,           "
        + "\n  CAST( NULL    AS FLOAT              ) AS mdrOptFLOAT,          "
        + "\n  CAST(  3.3    AS DOUBLE             ) AS mdrReqDOUBLE,         "
        + "\n  "
        + "\n  CAST(  4.4    AS DECIMAL(5,3)       ) AS mdrReqDECIMAL_5_3,    "
        + "\n  "
        + "\n  CAST( 'Hi'    AS VARCHAR(10)        ) AS mdrReqVARCHAR_10,     "
        + "\n  CAST( NULL    AS VARCHAR            ) AS mdrOptVARCHAR,        "
        + "\n  CAST( '55'    AS CHAR(5)            ) AS mdrReqCHAR_5,         "
        + "\n  CAST( NULL    AS VARBINARY(16)      ) AS mdrOptVARBINARY_16,   "
        + "\n  CAST( NULL    AS VARBINARY(65536)   ) AS mdrOptBINARY_65536,   "
        + "\n  CAST( NULL    AS BINARY(8)          ) AS mdrOptBINARY_8,       "
        + "\n  "
        + "\n                   DATE '2015-01-01'    AS mdrReqDATE,           "
        + "\n                   TIME '23:59:59'      AS mdrReqTIME,           "
        + "\n  CAST( NULL    AS TIME(7)            ) AS mdrOptTIME_7,         "
        + "\n  CAST( NULL    AS TIMESTAMP          ) AS mdrOptTIMESTAMP,      "
        + "\n  INTERVAL '1'     YEAR                 AS mdrReqINTERVAL_Y,     "
        + "\n  INTERVAL '1-2'   YEAR(3) TO MONTH     AS mdrReqINTERVAL_3Y_Mo, "
        + "\n  INTERVAL '2'     MONTH                AS mdrReqINTERVAL_Mo,    "
        + "\n  INTERVAL '3'     DAY                  AS mdrReqINTERVAL_D,     "
        + "\n  INTERVAL '3 4'   DAY(4) TO HOUR       AS mdrReqINTERVAL_4D_H,  "
        + "\n  INTERVAL '3 4:5' DAY(3) TO MINUTE     AS mdrReqINTERVAL_3D_Mi, "
        + "\n  INTERVAL '3 4:5:6' DAY(2) TO SECOND(5) AS mdrReqINTERVAL_2D_S5, "
        + "\n  INTERVAL '4'     HOUR                 AS mdrReqINTERVAL_H,     "
        + "\n  INTERVAL '4:5'   HOUR(1) TO MINUTE    AS mdrReqINTERVAL_1H_Mi, "
        + "\n  INTERVAL '4:5:6' HOUR(3) TO SECOND(1) AS mdrReqINTERVAL_3H_S1, "
        + "\n  INTERVAL '5'     MINUTE               AS mdrReqINTERVAL_Mi,    "
        + "\n  INTERVAL '5:6'   MINUTE(5) TO SECOND  AS mdrReqINTERVAL_5Mi_S, "
        + "\n  INTERVAL '6'     SECOND               AS mdrReqINTERVAL_S,     "
        + "\n  INTERVAL '6'     SECOND(3)            AS mdrReqINTERVAL_3S,    "
        + "\n  INTERVAL '6'     SECOND(3, 1)         AS mdrReqINTERVAL_3S1,   "
        + "\n  '' "
        + "\nFROM INFORMATION_SCHEMA.COLUMNS "
        + "\nLIMIT 1 " );
    assertTrue( util.next() );
    assertTrue("Error creating temporary test-columns view " + VIEW_NAME + ": "
          + util.getString( 2 ), util.getBoolean( 1 ) );

    // Set up result rows for temporary test view and Hivetest columns:

    mdrOptBOOLEAN        = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptBOOLEAN" );

    // TODO(DRILL-2470): Uncomment when TINYINT is implemented:
    //mdrReqTINYINT        = setUpRow( VIEW_SCHEMA, VIEW_NAME, "mdrReqTINYINT" );
    // TODO(DRILL-2470): Uncomment when SMALLINT is implemented:
    //mdrOptSMALLINT       = setUpRow( VIEW_SCHEMA, VIEW_NAME, "mdrOptSMALLINT" );
    mdrReqINTEGER        = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTEGER" );
    mdrOptBIGINT         = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptBIGINT" );

    // TODO(DRILL-2683): Uncomment when REAL is implemented:
    //mdrOptREAL           = setUpRow( VIEW_SCHEMA, VIEW_NAME, "mdrOptREAL" );
    mdrOptFLOAT          = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptFLOAT" );
    mdrReqDOUBLE         = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqDOUBLE" );

    mdrReqDECIMAL_5_3    = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqDECIMAL_5_3" );

    mdrReqVARCHAR_10     = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqVARCHAR_10" );
    mdrOptVARCHAR        = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptVARCHAR" );
    mdrReqCHAR_5         = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqCHAR_5" );
    mdrOptVARBINARY_16   = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptVARBINARY_16" );
    mdrOptBINARY_65536   = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptBINARY_65536" );

    mdrReqDATE           = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqDATE" );
    mdrReqTIME           = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqTIME" );
    mdrOptTIME_7         = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptTIME_7" );
    mdrOptTIMESTAMP      = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrOptTIMESTAMP" );

    mdrReqINTERVAL_Y     = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_Y" );
    mdrReqINTERVAL_3Y_Mo = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_3Y_Mo" );
    mdrReqINTERVAL_Mo    = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_Mo" );
    mdrReqINTERVAL_D     = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_D" );
    mdrReqINTERVAL_4D_H  = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_4D_H" );
    mdrReqINTERVAL_3D_Mi = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_3D_Mi" );
    mdrReqINTERVAL_2D_S5 = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_2D_S5" );
    mdrReqINTERVAL_H     = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_H" );
    mdrReqINTERVAL_1H_Mi = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_1H_Mi" );
    mdrReqINTERVAL_3H_S1 = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_3H_S1" );
    mdrReqINTERVAL_Mi    = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_Mi" );
    mdrReqINTERVAL_5Mi_S = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_5Mi_S" );
    mdrReqINTERVAL_S     = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_S" );
    mdrReqINTERVAL_3S    = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_3S" );
    mdrReqINTERVAL_3S1   = setUpRow( DFS_TMP_SCHEMA, VIEW_NAME, "mdrReqINTERVAL_3S1" );

    /* TODO(DRILL-3253)(start): Update this once we have test plugin supporting all needed types.
    mdrReqARRAY   = setUpRow( "hive_test.default", "infoschematest", "listtype" );
    mdrReqMAP     = setUpRow( "hive_test.default", "infoschematest", "maptype" );
    mdrUnkSTRUCT = setUpRow( "hive_test.default", "infoschematest", "structtype" );
    mdrUnkUnion  = setUpRow( "hive_test.default", "infoschematest", "uniontypetype" );
    TODO(DRILL-3253)(end) */

    // Set up getColumns(...)) result set' metadata:

    // Get all columns for more diversity of values (e.g., nulls and non-nulls),
    // in case that affects metadata (e.g., nullability) (in future).
    final ResultSet allColumns = dbMetadata.getColumns( null /* any catalog */,
                                                        null /* any schema */,
                                                        "%"  /* any table */,
                                                        "%"  /* any column */ );
    rowsMetadata = allColumns.getMetaData();
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    ResultSet util =
        connection.createStatement().executeQuery( "DROP VIEW " + VIEW_NAME + "" );
    assertTrue( util.next() );
    assertTrue( "Error dropping temporary test-columns view " + VIEW_NAME + ": "
                + util.getString( 2 ), util.getBoolean( 1 ) );
    connection.close();
  }


  private Integer getIntOrNull( ResultSet row, String columnName ) throws SQLException {
    final int value = row.getInt( columnName );
    return row.wasNull() ? null : Integer.valueOf( value );
  }


  //////////////////////////////////////////////////////////////////////
  // Tests:

  ////////////////////////////////////////////////////////////
  // Number of columns.

  @Test
  public void testMetadataHasRightNumberOfColumns() throws SQLException {
    // TODO:  Review:  Is this check valid?  (Are extra columns allowed?)
    assertThat( "column count", rowsMetadata.getColumnCount(), equalTo( 24 ) );
  }


  ////////////////////////////////////////////////////////////
  // #1: TABLE_CAT:
  // - JDBC:   "1. ... String => table catalog (may be null)"
  // - Drill:  Apparently chooses always "DRILL".
  // - (Meta): VARCHAR (NVARCHAR?); Non-nullable(?);

  @Test
  public void test_TABLE_CAT_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 1 ), equalTo( "TABLE_CAT" ) );
  }

  @Test
  public void test_TABLE_CAT_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "TABLE_CAT" ), equalTo( "DRILL" ) );
  }

  // Not bothering with other test columns for TABLE_CAT.

  @Test
  public void test_TABLE_CAT_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 1 ), equalTo( "TABLE_CAT" ) );
  }

  @Test
  public void test_TABLE_CAT_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 1 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_TABLE_CAT_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 1 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_TABLE_CAT_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 1 ),
                equalTo( String.class.getName() ) );
  }

  @Ignore( "until resolved:  any requirement on nullability (DRILL-2420?)" )
  @Test
  public void test_TABLE_CAT_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 1 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #2: TABLE_SCHEM:
  // - JDBC:   "2. ... String => table schema (may be null)"
  // - Drill:  Always reports a schema name.
  // - (Meta): VARCHAR (NVARCHAR?); Nullable?;

  @Test
  public void test_TABLE_SCHEM_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 2 ), equalTo( "TABLE_SCHEM" ) );
  }

  @Test
  public void test_TABLE_SCHEM_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "TABLE_SCHEM" ), equalTo( DFS_TMP_SCHEMA ) );
  }

  // Not bothering with other _local_view_ test columns for TABLE_SCHEM.

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_TABLE_SCHEM_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( mdrReqARRAY.getString( "TABLE_SCHEM" ),
                equalTo( "hive_test.default" ) );
  }

  // Not bothering with other Hive test columns for TABLE_SCHEM.

  @Test
  public void test_TABLE_SCHEM_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 2 ), equalTo( "TABLE_SCHEM" ) );
  }

  @Test
  public void test_TABLE_SCHEM_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 2 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_TABLE_SCHEM_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 2 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_TABLE_SCHEM_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 2 ),
                equalTo( String.class.getName() ) );
  }

  @Ignore( "until resolved:  any requirement on nullability (DRILL-2420?)" )
  @Test
  public void test_TABLE_SCHEM_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 2 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #3: TABLE_NAME:
  // - JDBC:  "3. ... String => table name"
  // - Drill:
  // - (Meta): VARCHAR (NVARCHAR?); Non-nullable?;

  @Test
  public void test_TABLE_NAME_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 3 ), equalTo( "TABLE_NAME" ) );
  }

  @Test
  public void test_TABLE_NAME_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "TABLE_NAME" ), equalTo( VIEW_NAME ) );
  }

  // Not bothering with other _local_view_ test columns for TABLE_NAME.

  @Test
  public void test_TABLE_NAME_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 3 ), equalTo( "TABLE_NAME" ) );
  }

  @Test
  public void test_TABLE_NAME_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 3 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_TABLE_NAME_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 3 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_TABLE_NAME_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 3 ),
                equalTo( String.class.getName() ) );
  }

  @Ignore( "until resolved:  any requirement on nullability (DRILL-2420?)" )
  @Test
  public void test_TABLE_NAME_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 3 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #4: COLUMN_NAME:
  // - JDBC:  "4. ... String => column name"
  // - Drill:
  // - (Meta): VARCHAR (NVARCHAR?); Non-nullable(?);

  @Test
  public void test_COLUMN_NAME_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 4 ), equalTo( "COLUMN_NAME" ) );
  }

  @Test
  public void test_COLUMN_NAME_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "COLUMN_NAME" ), equalTo( "mdrOptBOOLEAN" ) );
  }

  // Not bothering with other _local_view_ test columns for TABLE_SCHEM.

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_COLUMN_NAME_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( mdrReqARRAY.getString( "COLUMN_NAME" ), equalTo( "listtype" ) );
  }

  // Not bothering with other Hive test columns for TABLE_SCHEM.

  @Test
  public void test_COLUMN_NAME_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 4 ), equalTo( "COLUMN_NAME" ) );
  }

  @Test
  public void test_COLUMN_NAME_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 4 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_COLUMN_NAME_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 4 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_COLUMN_NAME_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 4 ),
                equalTo( String.class.getName() ) );
  }

  @Ignore( "until resolved:  any requirement on nullability (DRILL-2420?)" )
  @Test
  public void test_COLUMN_NAME_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 4 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #5: DATA_TYPE:
  // - JDBC:  "5. ... int => SQL type from java.sql.Types"
  // - Drill:
  // - (Meta): INTEGER(?);  Non-nullable(?);

  @Test
  public void test_DATA_TYPE_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 5 ), equalTo( "DATA_TYPE" ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( getIntOrNull( mdrOptBOOLEAN, "DATA_TYPE" ), equalTo( Types.BOOLEAN ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( getIntOrNull( mdrReqTINYINT, "DATA_TYPE" ), equalTo( Types.TINYINT ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptSMALLINT, "DATA_TYPE" ), equalTo( Types.SMALLINT ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTEGER, "DATA_TYPE" ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptBIGINT, "DATA_TYPE" ), equalTo( Types.BIGINT ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptREAL() throws SQLException {
    assertThat( getIntOrNull( mdrOptREAL, "DATA_TYPE" ), equalTo( Types.REAL ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( getIntOrNull( mdrOptFLOAT, "DATA_TYPE" ), equalTo( Types.FLOAT ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDOUBLE, "DATA_TYPE" ), equalTo( Types.DOUBLE ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( getIntOrNull( mdrReqDECIMAL_5_3, "DATA_TYPE" ), equalTo( Types.DECIMAL ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( getIntOrNull( mdrReqVARCHAR_10, "DATA_TYPE" ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARCHAR, "DATA_TYPE" ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( getIntOrNull( mdrReqCHAR_5, "DATA_TYPE" ), equalTo( Types.CHAR ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARBINARY_16, "DATA_TYPE" ), equalTo( Types.VARBINARY ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptBINARY_1048576CHECK() throws SQLException {
    assertThat( getIntOrNull(mdrOptBINARY_65536, "DATA_TYPE" ), equalTo( Types.VARBINARY ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqDATE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDATE, "DATA_TYPE" ), equalTo( Types.DATE ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqTIME() throws SQLException {
    assertThat( getIntOrNull( mdrReqTIME, "DATA_TYPE" ), equalTo( Types.TIME ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIME_7, "DATA_TYPE" ), equalTo( Types.TIME ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIMESTAMP, "DATA_TYPE" ), equalTo( Types.TIMESTAMP ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_Y, "DATA_TYPE" ), equalTo( Types.OTHER ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightValue_mdrReqINTERVAL_H_S3() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3H_S1, "DATA_TYPE" ), equalTo( Types.OTHER ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DATA_TYPE_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( getIntOrNull( mdrReqARRAY, "DATA_TYPE" ), equalTo( Types.ARRAY ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DATA_TYPE_hasRightValue_tbdMAP() throws SQLException {
    assertThat( "java.sql.Types.* type code",
                getIntOrNull( mdrReqMAP, "DATA_TYPE" ), equalTo( Types.OTHER ) );
    // To-do:  Determine which.
    assertThat( "java.sql.Types.* type code",
                getIntOrNull( mdrReqMAP, "DATA_TYPE" ), equalTo( Types.JAVA_OBJECT ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DATA_TYPE_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( getIntOrNull( mdrUnkSTRUCT, "DATA_TYPE" ), equalTo( Types.STRUCT ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DATA_TYPE_hasRightValue_tbdUnion() throws SQLException {
    assertThat( "java.sql.Types.* type code",
                getIntOrNull( mdrUnkUnion, "DATA_TYPE" ), equalTo( Types.OTHER ) );
    // To-do:  Determine which.
    assertThat( "java.sql.Types.* type code",
                getIntOrNull( mdrUnkUnion, "DATA_TYPE" ), equalTo( Types.JAVA_OBJECT ) );
  }

  @Test
  public void test_DATA_TYPE_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 5 ), equalTo( "DATA_TYPE" ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 5 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 5 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 5 ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_DATA_TYPE_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 5 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #6: TYPE_NAME:
  // - JDBC:  "6. ... String => Data source dependent type name, for a UDT the
  //     type name is fully qualified"
  // - Drill:
  // - (Meta): VARCHAR (NVARCHAR?); Non-nullable?;

  @Test
  public void test_TYPE_NAME_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 6 ), equalTo( "TYPE_NAME" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "TYPE_NAME" ), equalTo( "BOOLEAN" ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( mdrReqTINYINT.getString( "TYPE_NAME" ), equalTo( "TINYINT" ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( mdrOptSMALLINT.getString( "TYPE_NAME" ), equalTo( "SMALLINT" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( mdrReqINTEGER.getString( "TYPE_NAME" ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( mdrOptBIGINT.getString( "TYPE_NAME" ), equalTo( "BIGINT" ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptREAL() throws SQLException {
    assertThat( mdrOptREAL.getString( "TYPE_NAME" ), equalTo( "REAL" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( mdrOptFLOAT.getString( "TYPE_NAME" ), equalTo( "FLOAT" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( mdrReqDOUBLE.getString( "TYPE_NAME" ), equalTo( "DOUBLE" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( mdrReqDECIMAL_5_3.getString( "TYPE_NAME" ), equalTo( "DECIMAL" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( mdrReqVARCHAR_10.getString( "TYPE_NAME" ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat( mdrOptVARCHAR.getString( "TYPE_NAME" ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( mdrReqCHAR_5.getString( "TYPE_NAME" ), equalTo( "CHARACTER" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( mdrOptVARBINARY_16.getString( "TYPE_NAME" ),
                equalTo( "BINARY VARYING" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptBINARY_1048576CHECK() throws SQLException {
    assertThat( mdrOptBINARY_65536.getString( "TYPE_NAME" ),
                equalTo( "BINARY VARYING" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqDATE() throws SQLException {
    assertThat( mdrReqDATE.getString( "TYPE_NAME" ), equalTo( "DATE" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqTIME() throws SQLException {
    assertThat( mdrReqTIME.getString( "TYPE_NAME" ), equalTo( "TIME" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( mdrOptTIME_7.getString( "TYPE_NAME" ), equalTo( "TIME" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    assertThat( mdrOptTIMESTAMP.getString( "TYPE_NAME" ), equalTo( "TIMESTAMP" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    // (What SQL standard specifies for DATA_TYPE in INFORMATION_SCHEMA.COLUMNS:)
    assertThat( mdrReqINTERVAL_Y.getString( "TYPE_NAME" ), equalTo( "INTERVAL" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightValue_mdrReqINTERVAL_H_S3() throws SQLException {
    // (What SQL standard specifies for DATA_TYPE in INFORMATION_SCHEMA.COLUMNS:)
    assertThat( mdrReqINTERVAL_3H_S1.getString( "TYPE_NAME" ), equalTo( "INTERVAL" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_TYPE_NAME_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( mdrReqARRAY.getString( "TYPE_NAME" ), equalTo( "ARRAY" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_TYPE_NAME_hasRightValue_tbdMAP() throws SQLException {
    assertThat( mdrReqMAP.getString( "TYPE_NAME" ), equalTo( "MAP" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_TYPE_NAME_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( mdrUnkSTRUCT.getString( "TYPE_NAME" ), equalTo( "STRUCT" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_TYPE_NAME_hasRightValue_tbdUnion() throws SQLException {
    assertThat( mdrUnkUnion.getString( "TYPE_NAME" ), equalTo( "OTHER" ) );
    fail( "Expected value is not resolved yet." );
  }

  @Test
  public void test_TYPE_NAME_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 6 ), equalTo( "TYPE_NAME" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 6 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 6 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_TYPE_NAME_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 6 ),
                equalTo( String.class.getName() ) );
  }

  @Ignore( "until resolved:  any requirement on nullability (DRILL-2420?)" )
  @Test
  public void test_TYPE_NAME_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 6 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #7: COLUMN_SIZE:
  // - JDBC:  "7. ... int => column size."
  //     "The COLUMN_SIZE column specifies the column size for the given column.
  //      For numeric data, this is the maximum precision.
  //      For character data, this is the length in characters.
  //      For datetime datatypes, this is the length in characters of the String
  //        representation (assuming the maximum allowed precision of the
  //        fractional seconds component).
  //      For binary data, this is the length in bytes.
  //      For the ROWID datatype, this is the length in bytes.
  //      Null is returned for data types where the column size is not applicable."
  //   - "Maximum precision" seem to mean maximum number of digits that can
  //     appear.
  // - (Meta): INTEGER(?); Nullable;

  @Test
  public void test_COLUMN_SIZE_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 7 ), equalTo( "COLUMN_SIZE" ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( getIntOrNull( mdrOptBOOLEAN, "COLUMN_SIZE" ), equalTo(1) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqTINYINT() throws SQLException {
    // 8 bits
    assertThat( getIntOrNull( mdrReqTINYINT, "COLUMN_SIZE" ), equalTo( 8 ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptSMALLINT() throws SQLException {
    // 16 bits
    assertThat( getIntOrNull( mdrOptSMALLINT, "COLUMN_SIZE" ), equalTo( 16 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTEGER() throws SQLException {
    // 32 bits
    assertThat( getIntOrNull( mdrReqINTEGER, "COLUMN_SIZE" ), equalTo( 32 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptBIGINT() throws SQLException {
    // 64 bits
    assertThat( getIntOrNull( mdrOptBIGINT, "COLUMN_SIZE" ), equalTo( 64 ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptREAL() throws SQLException {
    // 24 bits of precision
    assertThat( getIntOrNull( mdrOptREAL, "COLUMN_SIZE" ), equalTo( 24 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptFLOAT() throws SQLException {
    // 24 bits of precision (same as REAL--current Drill behavior)
    assertThat( getIntOrNull( mdrOptFLOAT, "COLUMN_SIZE" ), equalTo( 24 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqDOUBLE() throws SQLException {
    // 53 bits of precision
    assertThat( getIntOrNull( mdrReqDOUBLE, "COLUMN_SIZE" ), equalTo( 53 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( getIntOrNull( mdrReqDECIMAL_5_3, "COLUMN_SIZE" ), equalTo( 5 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( getIntOrNull( mdrReqVARCHAR_10, "COLUMN_SIZE" ), equalTo( 10 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat(getIntOrNull(mdrOptVARCHAR, "COLUMN_SIZE"), equalTo(org.apache.drill.common.types.Types.MAX_VARCHAR_LENGTH));
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( getIntOrNull( mdrReqCHAR_5, "COLUMN_SIZE" ), equalTo( 5 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARBINARY_16, "COLUMN_SIZE" ), equalTo( 16 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptBINARY_1048576() throws SQLException {
    assertThat( getIntOrNull(mdrOptBINARY_65536, "COLUMN_SIZE" ), equalTo( 65536 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqDATE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDATE, "COLUMN_SIZE" ), equalTo( 10 ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqTIME() throws SQLException {
    assertThat( getIntOrNull( mdrReqTIME, "COLUMN_SIZE" ),
                equalTo( 8  /* HH:MM:SS */  ) );
  }

  @Ignore( "TODO(DRILL-3225): unignore when datetime precision is implemented" )
  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIME_7, "COLUMN_SIZE" ),
                equalTo( 8  /* HH:MM:SS */ + 1 /* '.' */ + 7 /* sssssss */ ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasINTERIMValue_mdrOptTIME_7() throws SQLException {
    assertThat( "When datetime precision is implemented, un-ignore above method and purge this.",
                getIntOrNull( mdrOptTIME_7, "COLUMN_SIZE" ),
                equalTo( 8  /* HH:MM:SS */ ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIMESTAMP, "COLUMN_SIZE" ),
                equalTo( 19 /* YYYY-MM-DDTHH:MM:SS */  ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_Y, "COLUMN_SIZE" ),
                equalTo( 4 ) );  // "P12Y"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_3Y_Mo() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3Y_Mo, "COLUMN_SIZE" ),
                equalTo( 8 ) );  // "P123Y12M"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_Mo() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_Mo, "COLUMN_SIZE" ),
                equalTo( 4 ) );  // "P12M"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_D() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_D, "COLUMN_SIZE" ),
                equalTo( 4 ) );  // "P12D"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_4D_H() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_4D_H, "COLUMN_SIZE" ),
                equalTo( 10 ) );  // "P1234DT12H"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_3D_Mi() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3D_Mi, "COLUMN_SIZE" ),
                equalTo( 12 ) );  // "P123DT12H12M"
  }

  //Fixed with Calcite update
  //@Ignore( "TODO(DRILL-3244): unignore when fractional secs. prec. is right" )
  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_2D_S5() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_2D_S5, "COLUMN_SIZE" ),
                equalTo( 20 ) );  // "P12DT12H12M12.12345S"
  }

  @Ignore( "Ignored after Calcite update" )
  @Test
  public void test_COLUMN_SIZE_hasINTERIMValue_mdrReqINTERVAL_2D_S5() throws SQLException {
    assertThat( "When DRILL-3244 fixed, un-ignore above method and purge this.",
                getIntOrNull( mdrReqINTERVAL_2D_S5, "COLUMN_SIZE" ),
                equalTo( 17 ) );  // "P12DT12H12M12.12S"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_3H() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_H, "COLUMN_SIZE" ),
                equalTo( 5 ) );  // "PT12H"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_4H_Mi() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_1H_Mi, "COLUMN_SIZE" ),
                equalTo( 7 ) );  // "PT1H12M"
  }

  //Fixed with Calcite update
  //@Ignore( "TODO(DRILL-3244): unignore when fractional secs. prec. is right" )
  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3H_S1, "COLUMN_SIZE" ),
                equalTo( 14 ) );  // "PT123H12M12.1S"
  }

  @Ignore( "Ignored after Calcite update" )
  @Test
  public void test_COLUMN_SIZE_hasINTERIMValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( "When DRILL-3244 fixed, un-ignore above method and purge this.",
                getIntOrNull( mdrReqINTERVAL_3H_S1, "COLUMN_SIZE" ),
                equalTo( 16 ) );  // "PT123H12M12.123S"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_Mi() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_Mi, "COLUMN_SIZE" ),
                equalTo( 5 ) );  // "PT12M"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_5Mi_S() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_5Mi_S, "COLUMN_SIZE" ),
                equalTo( 18 ) );  // "PT12345M12.123456S"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_S() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_S, "COLUMN_SIZE" ),
                equalTo( 12 ) );  // "PT12.123456S"
  }

  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_3S() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3S, "COLUMN_SIZE" ),
                equalTo( 13 ) );  // "PT123.123456S"
  }

  //Fixed with Calcite update
  //@Ignore( "TODO(DRILL-3244): unignore when fractional secs. prec. is right" )
  @Test
  public void test_COLUMN_SIZE_hasRightValue_mdrReqINTERVAL_3S1() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3S1, "COLUMN_SIZE" ),
                equalTo( 8 ) );  // "PT123.1S"
  }

  @Ignore( "Ignored after Calcite update" )
  @Test
  public void test_COLUMN_SIZE_hasINTERIMValue_mdrReqINTERVAL_3S1() throws SQLException {
    assertThat( "When DRILL-3244 fixed, un-ignore above method and purge this.",
                getIntOrNull( mdrReqINTERVAL_3S1, "COLUMN_SIZE" ),
                equalTo( 10 ) );  // "PT123.123S"
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_COLUMN_SIZE_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( getIntOrNull( mdrReqARRAY, "COLUMN_SIZE" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_COLUMN_SIZE_hasRightValue_tbdMAP() throws SQLException {
    assertThat( getIntOrNull( mdrReqMAP, "COLUMN_SIZE" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_COLUMN_SIZE_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( getIntOrNull( mdrUnkSTRUCT, "COLUMN_SIZE" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_COLUMN_SIZE_hasRightValue_tbdUnion() throws SQLException {
    assertThat( getIntOrNull( mdrUnkUnion, "COLUMN_SIZE" ), nullValue() );
  }

  @Test
  public void test_COLUMN_SIZE_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 7 ), equalTo( "COLUMN_SIZE" ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 7 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 7 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 7 ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_COLUMN_SIZE_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 7 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #8: BUFFER_LENGTH:
  // - JDBC:   "8. ... is not used"
  // - Drill:
  // - (Meta):

  // Since "unused," check only certain meta-metadata.

  @Test
  public void test_BUFFER_LENGTH_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 8 ), equalTo( "BUFFER_LENGTH" ) );
  }

  // No specific value or even type to check for.

  @Test
  public void test_BUFFER_LENGTH_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 8 ), equalTo( "BUFFER_LENGTH" ) );
  }


  ////////////////////////////////////////////////////////////
  // #9: DECIMAL_DIGITS:
  // - JDBC:  "9. ... int => the number of fractional digits. Null is
  //     returned for data types where DECIMAL_DIGITS is not applicable."
  //   - Resolve:  When exactly null?
  // - Drill:
  // - (Meta):  INTEGER(?); Nullable;

  @Test
  public void test_DECIMAL_DIGITS_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 9 ), equalTo( "DECIMAL_DIGITS" ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( getIntOrNull( mdrOptBOOLEAN, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( getIntOrNull( mdrReqTINYINT, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptSMALLINT, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTEGER, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptBIGINT, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptREAL() throws SQLException {
    assertThat( getIntOrNull( mdrOptREAL, "DECIMAL_DIGITS" ), equalTo( 7 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( getIntOrNull( mdrOptFLOAT, "DECIMAL_DIGITS" ), equalTo( 7 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDOUBLE, "DECIMAL_DIGITS" ), equalTo( 15 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( getIntOrNull( mdrReqDECIMAL_5_3, "DECIMAL_DIGITS" ), equalTo( 3 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( getIntOrNull( mdrReqVARCHAR_10, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARCHAR, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( getIntOrNull( mdrReqCHAR_5, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARBINARY_16, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptBINARY_1048576CHECK() throws SQLException {
    assertThat( getIntOrNull(mdrOptBINARY_65536, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqDATE() throws SQLException {
    // Zero because, per SQL spec.,  DATE doesn't (seem to) have a datetime
    // precision, but its DATETIME_PRECISION value must not be null.
    assertThat( getIntOrNull( mdrReqDATE, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqTIME() throws SQLException {
    // Zero is default datetime precision for TIME in SQL DATETIME_PRECISION.
    assertThat( getIntOrNull( mdrReqTIME, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Ignore( "TODO(DRILL-3225): unignore when datetime precision is implemented" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIME_7, "DECIMAL_DIGITS" ), equalTo( 7 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasINTERIMValue_mdrOptTIME_7() throws SQLException {
    assertThat( "When datetime precision is implemented, un-ignore above method and purge this.",
                getIntOrNull( mdrOptTIME_7, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Ignore( "TODO(DRILL-3225): unignore when datetime precision is implemented" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    // 6 is default datetime precision for TIMESTAMP.
    assertThat( getIntOrNull( mdrOptTIMESTAMP, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    assertThat( "When datetime precision is implemented, un-ignore above method and purge this.",
                getIntOrNull( mdrReqINTERVAL_Y, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_3Y_Mo() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3Y_Mo, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_Mo() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_Mo, "DECIMAL_DIGITS" ), equalTo( 0 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_D() throws SQLException {
    // 6 seems to be Drill's (Calcite's) choice (to use default value for the
    // fractional seconds precision for when SECOND _is_ present) since the SQL
    // spec. (ISO/IEC 9075-2:2011(E) 10.1 <interval qualifier>) doesn't seem to
    // specify the fractional seconds precision when SECOND is _not_ present.
    assertThat( getIntOrNull( mdrReqINTERVAL_D, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_4D_H() throws SQLException {
    // 6 seems to be Drill's (Calcite's) choice (to use default value for the
    // fractional seconds precision for when SECOND _is_ present) since the SQL
    // spec. (ISO/IEC 9075-2:2011(E) 10.1 <interval qualifier>) doesn't seem to
    // specify the fractional seconds precision when SECOND is _not_ present.
    assertThat( getIntOrNull( mdrReqINTERVAL_4D_H, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_3D_Mi() throws SQLException {
    // 6 seems to be Drill's (Calcite's) choice (to use default value for the
    // fractional seconds precision for when SECOND _is_ present) since the SQL
    // spec. (ISO/IEC 9075-2:2011(E) 10.1 <interval qualifier>) doesn't seem to
    // specify the fractional seconds precision when SECOND is _not_ present.
    assertThat( getIntOrNull( mdrReqINTERVAL_3D_Mi, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  //Fixed with Calcite update
  //@Ignore( "TODO(DRILL-3244): unignore when fractional secs. prec. is right" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_2D_S5() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_2D_S5, "DECIMAL_DIGITS" ), equalTo( 5 ) );
  }

  @Ignore( "Ignored after Calcite update" )
  @Test
  public void test_DECIMAL_DIGITS_hasINTERIMValue_mdrReqINTERVAL_2D_S5() throws SQLException {
    assertThat( "When DRILL-3244 fixed, un-ignore above method and purge this.",
                getIntOrNull( mdrReqINTERVAL_2D_S5, "DECIMAL_DIGITS" ), equalTo( 2 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_3H() throws SQLException {
    // 6 seems to be Drill's (Calcite's) choice (to use default value for the
    // fractional seconds precision for when SECOND _is_ present) since the SQL
    // spec. (ISO/IEC 9075-2:2011(E) 10.1 <interval qualifier>) doesn't seem to
    // specify the fractional seconds precision when SECOND is _not_ present.
    assertThat( getIntOrNull( mdrReqINTERVAL_H, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_1H_Mi() throws SQLException {
    // 6 seems to be Drill's (Calcite's) choice (to use default value for the
    // fractional seconds precision for when SECOND _is_ present) since the SQL
    // spec. (ISO/IEC 9075-2:2011(E) 10.1 <interval qualifier>) doesn't seem to
    // specify the fractional seconds precision when SECOND is _not_ present.
    assertThat( getIntOrNull( mdrReqINTERVAL_1H_Mi, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  //Fixed with Calcite update
  //@Ignore( "TODO(DRILL-3244): unignore when fractional secs. prec. is right" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3H_S1, "DECIMAL_DIGITS" ), equalTo( 1 ) );
  }

  @Ignore( "Ignored after Calcite update" )
  @Test
  public void test_DECIMAL_DIGITS_hasINTERIMValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( "When DRILL-3244 fixed, un-ignore above method and purge this.",
                getIntOrNull( mdrReqINTERVAL_3H_S1, "DECIMAL_DIGITS" ), equalTo( 3 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_Mi() throws SQLException {
    // 6 seems to be Drill's (Calcite's) choice (to use default value for the
    // fractional seconds precision for when SECOND _is_ present) since the SQL
    // spec. (ISO/IEC 9075-2:2011(E) 10.1 <interval qualifier>) doesn't seem to
    // specify the fractional seconds precision when SECOND is _not_ present.
    assertThat( getIntOrNull( mdrReqINTERVAL_Mi, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_5Mi_S() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_5Mi_S, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_S() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_S, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_3S() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3S, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasINTERIMValue_mdrReqINTERVAL_3S() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3S, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Ignore( "TODO(DRILL-3244): unignore when fractional secs. prec. is right" )
  @Test
  public void test_DECIMAL_DIGITS_hasRightValue_mdrReqINTERVAL_3S1() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3S, "DECIMAL_DIGITS" ), equalTo( 1 ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasINTERIMValue_mdrReqINTERVAL_3S1() throws SQLException {
    assertThat( "When DRILL-3244 fixed, un-ignore above method and purge this.",
                getIntOrNull( mdrReqINTERVAL_3S, "DECIMAL_DIGITS" ), equalTo( 6 ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DECIMAL_DIGITS_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( getIntOrNull( mdrReqARRAY, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DECIMAL_DIGITS_hasRightValue_tbdMAP() throws SQLException {
    assertThat( getIntOrNull( mdrReqMAP, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DECIMAL_DIGITS_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( getIntOrNull( mdrUnkSTRUCT, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_DECIMAL_DIGITS_hasRightValue_tbdUnion() throws SQLException {
    assertThat( getIntOrNull( mdrUnkUnion, "DECIMAL_DIGITS" ), nullValue() );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 9 ), equalTo( "DECIMAL_DIGITS" ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 9 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 9 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 9 ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_DECIMAL_DIGITS_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 9 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #10: NUM_PREC_RADIX:
  // - JDBC:  "10. ... int => Radix (typically either 10 or 2)"
  //   - Seems should be null for non-numeric, but unclear.
  // - Drill:  ?
  // - (Meta): INTEGER?; Nullable?;
  //
  // Note:  Some MS page says NUM_PREC_RADIX specifies the units (decimal digits
  // or binary bits COLUMN_SIZE, and is NULL for non-numeric columns.

  @Test
  public void test_NUM_PREC_RADIX_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 10 ), equalTo( "NUM_PREC_RADIX" ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( getIntOrNull( mdrOptBOOLEAN, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( getIntOrNull( mdrReqTINYINT, "NUM_PREC_RADIX" ), equalTo( 2 ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptSMALLINT, "NUM_PREC_RADIX" ), equalTo( 2 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTEGER, "NUM_PREC_RADIX" ), equalTo( 2 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptBIGINT, "NUM_PREC_RADIX" ), equalTo( 2 ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptREAL() throws SQLException {
    assertThat( getIntOrNull( mdrOptREAL, "NUM_PREC_RADIX" ), equalTo( 2 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( getIntOrNull( mdrOptFLOAT, "NUM_PREC_RADIX" ), equalTo( 2 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDOUBLE, "NUM_PREC_RADIX" ), equalTo( 2 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( getIntOrNull( mdrReqDECIMAL_5_3, "NUM_PREC_RADIX" ), equalTo( 10 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( getIntOrNull( mdrReqVARCHAR_10, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARCHAR, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( getIntOrNull( mdrReqCHAR_5, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARBINARY_16, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptBINARY_1048576CHECK() throws SQLException {
    assertThat( getIntOrNull(mdrOptBINARY_65536, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqDATE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDATE, "NUM_PREC_RADIX" ), equalTo( 10 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqTIME() throws SQLException {
    assertThat( getIntOrNull( mdrReqTIME, "NUM_PREC_RADIX" ), equalTo( 10 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIME_7, "NUM_PREC_RADIX" ), equalTo( 10 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIMESTAMP, "NUM_PREC_RADIX" ), equalTo( 10 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_Y, "NUM_PREC_RADIX" ), equalTo( 10 ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3H_S1, "NUM_PREC_RADIX" ), equalTo( 10 ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NUM_PREC_RADIX_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( getIntOrNull( mdrReqARRAY, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NUM_PREC_RADIX_hasRightValue_tbdMAP() throws SQLException {
    assertThat( getIntOrNull( mdrReqMAP, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NUM_PREC_RADIX_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( getIntOrNull( mdrUnkSTRUCT, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NUM_PREC_RADIX_hasRightValue_tbdUnion() throws SQLException {
    assertThat( getIntOrNull( mdrUnkUnion, "NUM_PREC_RADIX" ), nullValue() );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 10 ), equalTo( "NUM_PREC_RADIX" ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 10 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 10 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 10 ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_NUM_PREC_RADIX_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 10 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #11: NULLABLE:
  // - JDBC:  "11. ... int => is NULL allowed.
  //     columnNoNulls - might not allow NULL values
  //     columnNullable - definitely allows NULL values
  //     columnNullableUnknown - nullability unknown"
  // - Drill:
  // - (Meta): INTEGER(?); Non-nullable(?).

  @Test
  public void test_NULLABLE_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 11 ), equalTo( "NULLABLE" ) );
  }

  @Ignore( "until resolved:  any requirement on nullability (DRILL-2420?)" )
  @Test
  public void test_NULLABLE_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptBOOLEAN, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_NULLABLE_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqTINYINT, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_NULLABLE_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptSMALLINT, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptBIGINT, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_NULLABLE_hasRightValue_mdrOptREAL() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptREAL, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptFLOAT, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqDOUBLE, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqINTEGER, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqDECIMAL_5_3, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqVARCHAR_10, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptVARCHAR, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqCHAR_5, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptVARBINARY_16, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrOptBINARY_1048576() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull(mdrOptBINARY_65536, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqDATE() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqDATE, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqTIME() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqTIME, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptTIME_7, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrOptTIMESTAMP, "NULLABLE" ), equalTo( columnNullable ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqINTERVAL_Y, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  public void test_NULLABLE_hasRightValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrReqINTERVAL_3H_S1, "NULLABLE" ), equalTo( columnNoNulls ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NULLABLE_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( getIntOrNull( mdrReqARRAY, "NULLABLE" ), equalTo( columnNoNulls ) );
    // To-do:  Determine which.
    assertThat( getIntOrNull( mdrReqARRAY, "NULLABLE" ), equalTo( columnNullable ) );
    // To-do:  Determine which.
    assertThat( getIntOrNull( mdrReqARRAY, "NULLABLE" ), equalTo( columnNullableUnknown ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NULLABLE_hasRightValue_tbdMAP() throws SQLException {
    assertThat( getIntOrNull( mdrReqMAP, "NULLABLE" ), equalTo( columnNoNulls ) );
    // To-do:  Determine which.
    assertThat( getIntOrNull( mdrReqMAP, "NULLABLE" ), equalTo( columnNullable ) );
    // To-do:  Determine which.
    assertThat( getIntOrNull( mdrReqMAP, "NULLABLE" ), equalTo( columnNullableUnknown ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NULLABLE_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( getIntOrNull( mdrUnkSTRUCT, "NULLABLE" ), equalTo( columnNullable ) );
    // To-do:  Determine which.
    assertThat( getIntOrNull( mdrUnkSTRUCT, "NULLABLE" ), equalTo( columnNoNulls ) );
    // To-do:  Determine which.
    assertThat( getIntOrNull( mdrUnkSTRUCT, "NULLABLE" ), equalTo( columnNullableUnknown ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_NULLABLE_hasRightValue_tbdUnion() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrUnkUnion, "NULLABLE" ), equalTo( columnNullable ) );
    // To-do:  Determine which.
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrUnkUnion, "NULLABLE" ), equalTo( columnNoNulls ) );
    // To-do:  Determine which.
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                getIntOrNull( mdrUnkUnion, "NULLABLE" ), equalTo( columnNullableUnknown ) );
  }

  @Test
  public void test_NULLABLE_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 11 ), equalTo( "NULLABLE" ) );
  }

  @Test
  public void test_NULLABLE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 11 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_NULLABLE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 11 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_NULLABLE_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 11 ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_NULLABLE_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 11 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #12: REMARKS:
  // - JDBC:  "12. ... String => comment describing column (may be null)"
  // - Drill: none, so always null
  // - (Meta): VARCHAR (NVARCHAR?); Nullable;

  @Test
  public void test_REMARKS_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 12 ), equalTo( "REMARKS" ) );
  }

  @Test
  public void test_REMARKS_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "REMARKS" ), nullValue() );
  }

  @Test
  public void test_REMARKS_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 12 ), equalTo( "REMARKS" ) );
  }

  @Test
  public void test_REMARKS_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 12 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_REMARKS_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 12 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_REMARKS_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 12 ),
                equalTo( String.class.getName() ) );
  }

  @Test
  public void test_REMARKS_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 12 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #13: COLUMN_DEF:
  // - JDBC:  "13. ... String => default value for the column, which should be
  //     interpreted as a string when the value is enclosed in single quotes
  //     (may be null)"
  // - Drill:  no real default values, right?
  // - (Meta): VARCHAR (NVARCHAR?);  Nullable;

  @Test
  public void test_COLUMN_DEF_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 13 ), equalTo( "COLUMN_DEF" ) );
  }

  @Test
  public void test_COLUMN_DEF_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "COLUMN_DEF" ), nullValue() );
  }

  @Test
  public void test_COLUMN_DEF_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 13 ), equalTo( "COLUMN_DEF" ) );
  }

  @Test
  public void test_COLUMN_DEF_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 13 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_COLUMN_DEF_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 13 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_COLUMN_DEF_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 13 ),
                equalTo( String.class.getName() ) ); //???Text
  }

  @Test
  public void test_COLUMN_DEF_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 13 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #14: SQL_DATA_TYPE:
  // - JDBC:  "14. ... int => unused"
  // - Drill:
  // - (Meta): INTEGER(?);

  // Since "unused," check only certain meta-metadata.

  @Test
  public void test_SQL_DATA_TYPE_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 14 ), equalTo( "SQL_DATA_TYPE" ) );
  }

  // No specific value to check for.

  @Test
  public void test_SQL_DATA_TYPE_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 14 ), equalTo( "SQL_DATA_TYPE" ) );
  }

  @Test
  public void test_SQL_DATA_TYPE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 14 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_SQL_DATA_TYPE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 14 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_SQL_DATA_TYPE_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 14 ),
                equalTo( Integer.class.getName() ) );
  }


  ////////////////////////////////////////////////////////////
  // #15: SQL_DATETIME_SUB:
  // - JDBC:  "15. ... int => unused"
  // - Drill:
  // - (Meta):  INTEGER(?);

  // Since "unused," check only certain meta-metadata.

  @Test
  public void test_SQL_DATETIME_SUB_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 15 ), equalTo( "SQL_DATETIME_SUB" ) );
  }

  // No specific value to check for.

  @Test
  public void test_SQL_DATETIME_SUB_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 15 ), equalTo( "SQL_DATETIME_SUB" ) );
  }

  @Test
  public void test_SQL_DATETIME_SUB_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 15 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_SQL_DATETIME_SUB_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 15 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_SQL_DATETIME_SUB_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 15 ),
                equalTo( Integer.class.getName() ) );
  }


  ////////////////////////////////////////////////////////////
  // #16: CHAR_OCTET_LENGTH:
  // - JDBC:  "16. ... int => for char types the maximum number of bytes
  //     in the column"
  //   - apparently should be null for non-character types
  // - Drill:
  // - (Meta): INTEGER(?); Nullable(?);

  @Test
  public void test_CHAR_OCTET_LENGTH_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 16 ), equalTo( "CHAR_OCTET_LENGTH" ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( getIntOrNull( mdrOptBOOLEAN, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( getIntOrNull( mdrReqTINYINT, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptSMALLINT, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTEGER, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptREAL, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( getIntOrNull( mdrOptFLOAT, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDOUBLE, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( getIntOrNull( mdrReqDECIMAL_5_3, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( getIntOrNull( mdrReqVARCHAR_10, "CHAR_OCTET_LENGTH" ),
                equalTo( 10   /* chars. */
                         * 4  /* max. UTF-8 bytes per char. */ ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARCHAR, "CHAR_OCTET_LENGTH" ),
        equalTo(org.apache.drill.common.types.Types.MAX_VARCHAR_LENGTH /* chars. (default of 65535) */
                         * 4  /* max. UTF-8 bytes per char. */ ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( getIntOrNull( mdrReqCHAR_5, "CHAR_OCTET_LENGTH" ),
                equalTo( 5    /* chars. */
                         * 4  /* max. UTF-8 bytes per char. */ ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( getIntOrNull( mdrOptVARBINARY_16, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptBINARY_1048576CHECK() throws SQLException {
    assertThat( getIntOrNull(mdrOptBINARY_65536, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqDATE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDATE, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqTIME() throws SQLException {
    assertThat( getIntOrNull( mdrReqTIME, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIME_7, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    assertThat( getIntOrNull( mdrOptTIMESTAMP, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_Y, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTERVAL_3H_S1, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_CHAR_OCTET_LENGTH_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( getIntOrNull( mdrReqARRAY, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_CHAR_OCTET_LENGTH_hasRightValue_tbdMAP() throws SQLException {
    assertThat( getIntOrNull( mdrReqMAP, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_CHAR_OCTET_LENGTH_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( getIntOrNull( mdrUnkSTRUCT, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_CHAR_OCTET_LENGTH_hasRightValue_tbdUnion() throws SQLException {
    assertThat( getIntOrNull( mdrUnkUnion, "CHAR_OCTET_LENGTH" ), nullValue() );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 16 ), equalTo( "CHAR_OCTET_LENGTH" ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 16 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 16 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 16 ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_CHAR_OCTET_LENGTH_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 16 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #17: ORDINAL_POSITION:
  // - JDBC:  "17. ... int => index of column in table (starting at 1)"
  // - Drill:
  // - (Meta):  INTEGER(?); Non-nullable(?).

  @Test
  public void test_ORDINAL_POSITION_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 17 ), equalTo( "ORDINAL_POSITION" ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( getIntOrNull( mdrOptBOOLEAN, "ORDINAL_POSITION" ), equalTo( 1 ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( getIntOrNull( mdrReqTINYINT, "ORDINAL_POSITION" ), equalTo( 2 ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptSMALLINT, "ORDINAL_POSITION" ), equalTo( 3 ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( getIntOrNull( mdrReqINTEGER, "ORDINAL_POSITION" ), equalTo( 4 ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( getIntOrNull( mdrOptBIGINT, "ORDINAL_POSITION" ), equalTo( 5 ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrOptREAL() throws SQLException {
    assertThat( getIntOrNull( mdrOptREAL, "ORDINAL_POSITION" ), equalTo( 6 ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( getIntOrNull( mdrOptFLOAT, "ORDINAL_POSITION" ), equalTo( 7 ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( getIntOrNull( mdrReqDOUBLE, "ORDINAL_POSITION" ), equalTo( 8 ) );
  }

  @Test
    @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_ORDINAL_POSITION_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( getIntOrNull( mdrReqARRAY, "ORDINAL_POSITION" ), equalTo( 14 ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 17 ), equalTo( "ORDINAL_POSITION" ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 17 ), equalTo( "INTEGER" ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 17 ), equalTo( Types.INTEGER ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 17 ),
                equalTo( Integer.class.getName() ) );
  }

  @Test
  public void test_ORDINAL_POSITION_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 17 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #18: IS_NULLABLE:
  // - JDBC:  "18. ... String => ISO rules are used to determine the nullability for a column.
  //     YES --- if the column can include NULLs
  //     NO --- if the column cannot include NULLs
  //     empty string --- if the nullability for the column is unknown"
  // - Drill:  ?
  // - (Meta): VARCHAR (NVARCHAR?); Not nullable?

  @Test
  public void test_IS_NULLABLE_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 18 ), equalTo( "IS_NULLABLE" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when TINYINT is implemented" )
  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqTINYINT() throws SQLException {
    assertThat( mdrReqTINYINT.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Ignore( "TODO(DRILL-2470): unignore when SMALLINT is implemented" )
  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptSMALLINT() throws SQLException {
    assertThat( mdrOptSMALLINT.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqINTEGER() throws SQLException {
    assertThat( mdrReqINTEGER.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptBIGINT() throws SQLException {
    assertThat( mdrOptBIGINT.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Ignore( "TODO(DRILL-2683): unignore when REAL is implemented" )
  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptREAL() throws SQLException {
    assertThat( mdrOptREAL.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptFLOAT() throws SQLException {
    assertThat( mdrOptFLOAT.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqDOUBLE() throws SQLException {
    assertThat( mdrReqDOUBLE.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqDECIMAL_5_3() throws SQLException {
    assertThat( mdrReqDECIMAL_5_3.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqVARCHAR_10() throws SQLException {
    assertThat( mdrReqVARCHAR_10.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptVARCHAR() throws SQLException {
    assertThat( mdrOptVARCHAR.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqCHAR_5() throws SQLException {
    assertThat( mdrReqCHAR_5.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptVARBINARY_16() throws SQLException {
    assertThat( mdrOptVARBINARY_16.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptBINARY_1048576CHECK() throws SQLException {
    assertThat( mdrOptBINARY_65536.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqDATE() throws SQLException {
    assertThat( mdrReqDATE.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqTIME() throws SQLException {
    assertThat( mdrReqTIME.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptTIME_7() throws SQLException {
    assertThat( mdrOptTIME_7.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrOptTIMESTAMP() throws SQLException {
    assertThat( mdrOptTIMESTAMP.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqINTERVAL_Y() throws SQLException {
    assertThat( mdrReqINTERVAL_Y.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightValue_mdrReqINTERVAL_3H_S1() throws SQLException {
    assertThat( mdrReqINTERVAL_3H_S1.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_IS_NULLABLE_hasRightValue_tdbARRAY() throws SQLException {
    assertThat( mdrReqARRAY.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
    // To-do:  Determine which.
    assertThat( mdrReqARRAY.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
    // To-do:  Determine which.
    assertThat( mdrReqARRAY.getString( "IS_NULLABLE" ), equalTo( "" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_IS_NULLABLE_hasRightValue_tbdMAP() throws SQLException {
    assertThat( mdrReqMAP.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
    // To-do:  Determine which.
    assertThat( mdrReqMAP.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
    // To-do:  Determine which.
    assertThat( mdrReqMAP.getString( "IS_NULLABLE" ), equalTo( "" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_IS_NULLABLE_hasRightValue_tbdSTRUCT() throws SQLException {
    assertThat( mdrUnkSTRUCT.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
    // To-do:  Determine which.
    assertThat( mdrUnkSTRUCT.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
    // To-do:  Determine which.
    assertThat( mdrUnkSTRUCT.getString( "IS_NULLABLE" ), equalTo( "" ) );
  }

  @Test
  @Ignore( "TODO(DRILL-3253): unignore when we have all-types test storage plugin" )
  public void test_IS_NULLABLE_hasRightValue_tbdUnion() throws SQLException {
    assertThat( mdrUnkUnion.getString( "IS_NULLABLE" ), equalTo( "YES" ) );
    // To-do:  Determine which.
    assertThat( mdrUnkUnion.getString( "IS_NULLABLE" ), equalTo( "NO" ) );
    // To-do:  Determine which.
    assertThat( mdrUnkUnion.getString( "IS_NULLABLE" ), equalTo( "" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 18 ), equalTo( "IS_NULLABLE" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 18 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 18 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_IS_NULLABLE_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 18 ),
                equalTo( String.class.getName() ) );
  }

  @Ignore( "until resolved:  any requirement on nullability (DRILL-2420?)" )
  @Test
  public void test_IS_NULLABLE_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 18 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #19: SCOPE_CATALOG:
  // - JDBC:  "19. ... String => catalog of table that is the scope of a
  //     reference attribute (null if DATA_TYPE isn't REF)"
  // - Drill:
  // - (Meta): VARCHAR (NVARCHAR?); Nullable;

  @Test
  public void test_SCOPE_CATALOG_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 19 ), equalTo( "SCOPE_CATALOG" ) );
  }

  @Test
  public void test_SCOPE_CATALOG_hasRightValue_mdrOptBOOLEAN() throws SQLException {
      final String value = mdrOptBOOLEAN.getString( "SCOPE_SCHEMA" );
        assertThat( value, nullValue() );
    }

  @Test
  public void test_SCOPE_CATALOG_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 19 ), equalTo( "SCOPE_CATALOG" ) );
  }

  @Test
  public void test_SCOPE_CATALOG_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 19 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_SCOPE_CATALOG_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 19 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_SCOPE_CATALOG_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 19 ),
                equalTo( String.class.getName() ) );
  }

  @Test
  public void test_SCOPE_CATALOG_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 19 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #20: SCOPE_SCHEMA:
  // - JDBC:  "20. ... String => schema of table that is the scope of a
  //     reference attribute (null if the DATA_TYPE isn't REF)"
  // - Drill:  no REF, so always null?
  // - (Meta): VARCHAR?; Nullable;

  @Test
  public void test_SCOPE_SCHEMA_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 20 ), equalTo( "SCOPE_SCHEMA" ) );
  }

  @Test
  public void test_SCOPE_SCHEMA_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    final String value = mdrOptBOOLEAN.getString( "SCOPE_SCHEMA" );
    assertThat( value, nullValue() );
  }

  @Test
  public void test_SCOPE_SCHEMA_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 20 ), equalTo( "SCOPE_SCHEMA" ) );
  }

  @Test
  public void test_SCOPE_SCHEMA_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 20 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_SCOPE_SCHEMA_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 20 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_SCOPE_SCHEMA_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 20 ),
                equalTo( String.class.getName() ) );
  }

  @Test
  public void test_SCOPE_SCHEMA_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 20 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #21: SCOPE_TABLE:
  // - JDBC:  "21. ... String => table name that this the scope of a reference
  //     attribute (null if the DATA_TYPE isn't REF)"
  // - Drill:
  // - (Meta): VARCHAR (NVARCHAR?); Nullable;

  @Test
  public void test_SCOPE_TABLE_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 21 ), equalTo( "SCOPE_TABLE" ) );
  }

  @Test
  public void test_SCOPE_TABLE_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    final String value = mdrOptBOOLEAN.getString( "SCOPE_TABLE" );
    assertThat( value, nullValue() );
  }

  @Test
  public void test_SCOPE_TABLE_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 21 ), equalTo( "SCOPE_TABLE" ) );
  }

  @Test
  public void test_SCOPE_TABLE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 21 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_SCOPE_TABLE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 21 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_SCOPE_TABLE_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 21 ),
                equalTo( String.class.getName() ) );
  }

  @Test
  public void test_SCOPE_TABLE_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 21 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #22: SOURCE_DATA_TYPE:
  // - JDBC:  "22. ... short => source type of a distinct type or user-generated
  //     Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't
  //     DISTINCT or user-generated REF)"
  // - Drill:  not DISTINCT or REF, so null?
  // - (Meta): SMALLINT(?);  Nullable;

  @Test
  public void test_SOURCE_DATA_TYPE_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 22 ), equalTo( "SOURCE_DATA_TYPE" ) );
  }

  @Test
  public void test_SOURCE_DATA_TYPE_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    assertThat( mdrOptBOOLEAN.getString( "SOURCE_DATA_TYPE" ), nullValue() );
  }

  @Test
  public void test_SOURCE_DATA_TYPE_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 22 ), equalTo( "SOURCE_DATA_TYPE" ) );
  }

  @Test
  public void test_SOURCE_DATA_TYPE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 22 ), equalTo( "SMALLINT" ) );
  }

  @Test
  public void test_SOURCE_DATA_TYPE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 22 ), equalTo( Types.SMALLINT ) );
  }

  @Test
  public void test_SOURCE_DATA_TYPE_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 22 ),
                equalTo( Short.class.getName() ) );
  }

  @Test
  public void test_SOURCE_DATA_TYPE_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 22 ), equalTo( columnNullable ) );
  }


  ////////////////////////////////////////////////////////////
  // #23: IS_AUTOINCREMENT:
  // - JDBC:  "23. ... String => Indicates whether this column is auto incremented
  //     YES --- if the column is auto incremented
  //     NO --- if the column is not auto incremented
  //     empty string --- if it cannot be determined whether the column is auto incremented"
  // - Drill:
  // - (Meta): VARCHAR (NVARCHAR?); Non-nullable(?);

  @Test
  public void test_IS_AUTOINCREMENT_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 23 ), equalTo( "IS_AUTOINCREMENT" ) );
  }

  @Test
  public void test_IS_AUTOINCREMENT_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    // TODO:  Can it be 'NO' (not auto-increment) rather than '' (unknown)?
    assertThat( mdrOptBOOLEAN.getString( "IS_AUTOINCREMENT" ), equalTo( "" ) );
  }

  // Not bothering with other test columns for IS_AUTOINCREMENT.

  @Test
  public void test_IS_AUTOINCREMENT_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 23 ), equalTo( "IS_AUTOINCREMENT" ) );
  }

  @Test
  public void test_IS_AUTOINCREMENT_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 23 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_IS_AUTOINCREMENT_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 23 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_IS_AUTOINCREMENT_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 23 ),
                equalTo( String.class.getName() ) );
  }

  @Test
  public void test_IS_AUTOINCREMENT_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 23 ), equalTo( columnNoNulls ) );
  }


  ////////////////////////////////////////////////////////////
  // #24: IS_GENERATEDCOLUMN:
  // - JDBC:  "24. ... String => Indicates whether this is a generated column
  //     YES --- if this a generated column
  //     NO --- if this not a generated column
  //     empty string --- if it cannot be determined whether this is a generated column"
  // - Drill:
  // - (Meta): VARCHAR (NVARCHAR?); Non-nullable(?)

  @Test
  public void test_IS_GENERATEDCOLUMN_isAtRightPosition() throws SQLException {
    assertThat( rowsMetadata.getColumnLabel( 24 ), equalTo( "IS_GENERATEDCOLUMN" ) );
  }

  @Test
  public void test_IS_GENERATEDCOLUMN_hasRightValue_mdrOptBOOLEAN() throws SQLException {
    // TODO:  Can it be 'NO' (not auto-increment) rather than '' (unknown)?
    assertThat( mdrOptBOOLEAN.getString( "IS_GENERATEDCOLUMN" ), equalTo( "" ) );
  }

  // Not bothering with other test columns for IS_GENERATEDCOLUMN.

  @Test
  public void test_IS_GENERATEDCOLUMN_hasSameNameAndLabel() throws SQLException {
    assertThat( rowsMetadata.getColumnName( 24 ), equalTo( "IS_GENERATEDCOLUMN" ) );
  }

  @Test
  public void test_IS_GENERATEDCOLUMN_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 24 ),
                equalTo( "CHARACTER VARYING" ) );
  }

  @Test
  public void test_IS_GENERATEDCOLUMN_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 24 ), equalTo( Types.VARCHAR ) );
  }

  @Test
  public void test_IS_GENERATEDCOLUMN_hasRightClass() throws SQLException {

    assertThat( rowsMetadata.getColumnClassName( 24 ),
                equalTo( String.class.getName() ) );
  }

  @Test
  public void test_IS_GENERATEDCOLUMN_hasRightNullability() throws SQLException {
    assertThat( "ResultSetMetaData.column...Null... nullability code:",
                rowsMetadata.isNullable( 24 ), equalTo( columnNoNulls ) );
  }

} // class DatabaseMetaGetColumnsDataTest
