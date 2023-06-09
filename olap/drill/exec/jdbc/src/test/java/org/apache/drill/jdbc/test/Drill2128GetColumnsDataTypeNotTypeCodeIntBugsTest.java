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

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.drill.test.TestTools;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.jdbc.JdbcTestBase;

/**
 * Basic (spot-check/incomplete) tests for DRILL-2128 bugs (many
 * DatabaseMetaData.getColumns(...) result table problems).
 */
@Category(JdbcTest.class)
public class Drill2128GetColumnsDataTypeNotTypeCodeIntBugsTest extends JdbcTestBase {

  private static Connection connection;
  private static DatabaseMetaData dbMetadata;

  @Rule
  public final TestRule TIMEOUT = TestTools.getTimeoutRule( 120_000 /* ms */ );

  @BeforeClass
  public static void setUpConnection() throws Exception {
    // Get JDBC connection to Drill:
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses connection (and dependent DatabaseMetaData object) across
    // methods.)
    Driver.load();
    connection = DriverManager.getConnection( "jdbc:drill:zk=local" );
    dbMetadata = connection.getMetaData();
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    connection.close();
  }


  /**
   * Basic test that column DATA_TYPE is integer type codes (not strings such
   * as "VARCHAR" or "INTEGER").
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testColumn_DATA_TYPE_isInteger() throws Exception {
    // Get metadata for some column(s).
    final ResultSet columns = dbMetadata.getColumns( null, null, null, null );
    assertTrue( "DatabaseMetaData.getColumns(...) returned no rows", columns.next() );

    do {
      // DATA_TYPE should be INTEGER, so getInt( "DATA_TYPE" ) should succeed:
      final int typeCode1 = columns.getInt( "DATA_TYPE" );

      // DATA_TYPE should be at ordinal position 5 (seemingly):
      assertThat( "Column 5's label",
                  columns.getMetaData().getColumnLabel( 5 ), equalTo( "DATA_TYPE" ) );

      // Also, getInt( 5 ) should succeed and return the same type code as above:
      final int typeCode2 = columns.getInt( 5 );
      assertThat( "getInt( 5 ) (expected to be same as getInt( \"DATA_TYPE\" ))",
                  typeCode2, equalTo( typeCode1 ) );

      // Type code should be one of java.sql.Types.*:
      assertThat(
          typeCode1,
          anyOf( // List is from java.sql.Types
                 equalTo( Types.ARRAY ),
                 equalTo( Types.BIGINT ),
                 equalTo( Types.BINARY ),
                 equalTo( Types.BIT ),
                 equalTo( Types.BLOB ),
                 equalTo( Types.BOOLEAN ),
                 equalTo( Types.CHAR ),
                 equalTo( Types.CLOB ),
                 equalTo( Types.DATALINK ),
                 equalTo( Types.DATE ),
                 equalTo( Types.DECIMAL ),
                 equalTo( Types.DISTINCT ),
                 equalTo( Types.DOUBLE ),
                 equalTo( Types.FLOAT ),
                 equalTo( Types.INTEGER ),
                 equalTo( Types.JAVA_OBJECT ),
                 equalTo( Types.LONGNVARCHAR ),
                 equalTo( Types.LONGVARBINARY ),
                 equalTo( Types.LONGVARCHAR ),
                 equalTo( Types.NCHAR ),
                 equalTo( Types.NCLOB ),
                 // TODO:  Resolve:  Is it not clear whether Types.NULL can re-
                 // present a type (e.g., the type of NULL), or whether a column
                 // can ever have that type, and therefore whether Types.NULL
                 // can appear.  Currently, exclude NULL so we'll notice if it
                 // does appear:
                 // No equalTo( Types.NULL ).
                 equalTo( Types.NUMERIC ),
                 equalTo( Types.NVARCHAR ),
                 equalTo( Types.OTHER ),
                 equalTo( Types.REAL ),
                 equalTo( Types.REF ),
                 equalTo( Types.ROWID ),
                 equalTo( Types.SMALLINT ),
                 equalTo( Types.SQLXML ),
                 equalTo( Types.STRUCT ),
                 equalTo( Types.TIME ),
                 equalTo( Types.TIMESTAMP ),
                 equalTo( Types.TINYINT ),
                 equalTo( Types.VARBINARY ),
                 equalTo( Types.VARCHAR )
              ) );
    } while ( columns.next() );
  }

  /**
   * Basic test that column TYPE_NAME exists and is strings (such "INTEGER").
   */
  @Test
  public void testColumn_TYPE_NAME_isString() throws Exception {
    // Get metadata for some INTEGER column.
    final ResultSet columns =
        dbMetadata.getColumns( null, "INFORMATION_SCHEMA", "COLUMNS",
                               "ORDINAL_POSITION" );
    assertTrue( "DatabaseMetaData.getColumns(...) returned no rows", columns.next() );

    // TYPE_NAME should be character string for type name "INTEGER", so
    // getString( "TYPE_NAME" ) should succeed and getInt( "TYPE_NAME" ) should
    // fail:
    final String typeName1 = columns.getString( "TYPE_NAME" );
    assertThat( "getString( \"TYPE_NAME\" )", typeName1, equalTo( "INTEGER" ) );

    try {
      final int unexpected = columns.getInt( "TYPE_NAME"  );
      fail( "getInt( \"TYPE_NAME\" ) didn't throw exception (and returned "
            + unexpected + ")" );
    }
    catch ( SQLException e ) {
      // Expected.
    }

    // TYPE_NAME should be at ordinal position 6 (seemingly):
    assertThat( "Column 6's label",
                columns.getMetaData().getColumnLabel( 6 ), equalTo( "TYPE_NAME" ) );

    // Also, getString( 6 ) should succeed and return the same type name as above:
    final String typeName2 = columns.getString( 6 );
    assertThat( "getString( 6 ) (expected to be same as getString( \"TYPE_NAME\" ))",
                  typeName2, equalTo( typeName1 ) );
  }
}
