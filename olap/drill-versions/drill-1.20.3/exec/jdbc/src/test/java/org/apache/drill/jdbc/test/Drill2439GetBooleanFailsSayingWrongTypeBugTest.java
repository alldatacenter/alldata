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
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.drill.jdbc.JdbcTestBase;
import org.junit.experimental.categories.Category;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Category(JdbcTest.class)
public class Drill2439GetBooleanFailsSayingWrongTypeBugTest extends JdbcTestBase {

  private static Connection connection;
  private static Statement statement;

  @BeforeClass
  public static void setUpConnection() throws SQLException {
    connection = connect();
    statement = connection.createStatement();
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    connection.close();
  }

  @Test
  public void testGetBooleanGetsTrue() throws Exception {
    ResultSet rs =
        statement.executeQuery( "SELECT TRUE FROM INFORMATION_SCHEMA.CATALOGS" );
    rs.next();
    assertThat( "getBoolean(...) for TRUE", rs.getBoolean( 1 ), equalTo( true ) );
    assertThat( "wasNull", rs.wasNull(), equalTo( false ) );
  }

  @Test
  public void testGetBooleanGetsFalse() throws Exception {
    ResultSet rs =
        statement.executeQuery( "SELECT FALSE FROM INFORMATION_SCHEMA.CATALOGS" );
    rs.next();
    assertThat( "getBoolean(...) for FALSE", rs.getBoolean( 1 ), equalTo( false ) );
    assertThat( "wasNull", rs.wasNull(), equalTo( false ) );
  }

  @Test
  public void testGetBooleanGetsNull() throws Exception {
    ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS BOOLEAN ) FROM INFORMATION_SCHEMA.CATALOGS" );
    rs.next();
    assertThat( "getBoolean(...) for BOOLEAN NULL", rs.getBoolean( 1 ), equalTo( false ) );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }

}
