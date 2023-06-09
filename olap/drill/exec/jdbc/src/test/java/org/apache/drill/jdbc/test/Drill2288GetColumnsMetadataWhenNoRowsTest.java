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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.drill.jdbc.Driver;
import org.apache.drill.categories.JdbcTest;
import org.apache.drill.jdbc.JdbcTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

 /**
 * Tests from DRILL-2288, in which schema information wasn't propagated when a
 * scan yielded an empty (zero-row) result set.
 */
@Category(JdbcTest.class)
public class Drill2288GetColumnsMetadataWhenNoRowsTest extends JdbcTestBase {
  private static Connection connection;

  @BeforeClass
  public static void setUpConnection() throws SQLException {
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = new Driver().connect( "jdbc:drill:zk=local", JdbcTestBase.getDefaultProperties());
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    connection.close();
  }


  /**
   * Tests that an empty JSON file (having zero records) no longer triggers
   * breakage in schema propagation.  (Case failed before; columns a, b and c
   * didn't show up.)
   */
  @Test
  public void testEmptyJsonFileDoesntSuppressNetSchema1() throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet results = stmt.executeQuery( "SELECT a, b, c, * FROM cp.`empty.json`" );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been > 0",
                metadata.getColumnCount(), not( equalTo( 0 ) ) );

    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }

  @Test
  public void testEmptyJsonFileDoesntSuppressNetSchema2() throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet results = stmt.executeQuery( "SELECT a FROM cp.`empty.json`" );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been 1",
                metadata.getColumnCount(), equalTo( 1 ) );

    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }

  /**
   * Tests that an INFORMATION_SCHEMA.TABLES query that has zero rows because of
   * a (simple-enough) filter expression using column TABLE_SCHEMA (which
   * supports pushdown) still has all columns.  (Case failed before; had zero
   * columns.)
   */
  @Test
  public void testInfoSchemaTablesZeroRowsBy_TABLE_SCHEMA_works() throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet results =
        stmt.executeQuery( "SELECT * FROM INFORMATION_SCHEMA.`TABLES`"
                           + " WHERE TABLE_SCHEMA = ''" );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been > 0",
                metadata.getColumnCount(), not( equalTo( 0 ) ) );

    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }

  /** (Worked before (because TABLE_CATALOG test not pushed down).) */
  @Test
  public void testInfoSchemaTablesZeroRowsBy_TABLE_CATALOG_works() throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet results =
        stmt.executeQuery( "SELECT * FROM INFORMATION_SCHEMA.`TABLES`"
                           + " WHERE TABLE_CATALOG = ''" );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been > 0",
                metadata.getColumnCount(), not( equalTo( 0 ) ) );

    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }

  /** (Failed before (because TABLE_NAME test is pushed down).) */
  @Test
  public void testInfoSchemaTablesZeroRowsBy_TABLE_NAME_works()
      throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet results =
        stmt.executeQuery(
            "SELECT * FROM INFORMATION_SCHEMA.`TABLES` WHERE TABLE_NAME = ''" );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been > 0",
                metadata.getColumnCount(), not( equalTo( 0 ) ) );

    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }

  /** (Worked before.) */
  @Test
  public void testInfoSchemaTablesZeroRowsByLimitWorks() throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet results =
        stmt.executeQuery(
            "SELECT * FROM INFORMATION_SCHEMA.`TABLES` LIMIT 0" );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been > 0",
                metadata.getColumnCount(), not( equalTo( 0 ) ) );

    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }

  /** (Worked before.) */
  @Test
  public void testInfoSchemaTablesZeroRowsByWhereFalseWorks() throws Exception {
    Statement stmt = connection.createStatement();
    ResultSet results =
        stmt.executeQuery(
            "SELECT * FROM INFORMATION_SCHEMA.`TABLES` WHERE FALSE" );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been > 0",
                metadata.getColumnCount(), not( equalTo( 0 ) ) );

    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }

  /** (Failed before (because table schema and name tests are pushed down).) */
  @Test
  public void testGetTablesZeroRowsByTableSchemaOrNameWorks() throws Exception {
    DatabaseMetaData dbMetadata = connection.getMetaData();

    ResultSet results = dbMetadata.getTables( "NoSuchCatalog", "NoSuchSchema",
                                              "NoSuchTable", new String[0] );

    // Result set should still have columns even though there are no rows:
    ResultSetMetaData metadata = results.getMetaData();
    assertThat( "ResultSetMetaData.getColumnCount() should have been > 0",
                metadata.getColumnCount(), not( equalTo( 0 ) ) );
    assertThat( "Unexpected non-empty results.  Test rot?",
                false, equalTo( results.next() ) );
  }
}
