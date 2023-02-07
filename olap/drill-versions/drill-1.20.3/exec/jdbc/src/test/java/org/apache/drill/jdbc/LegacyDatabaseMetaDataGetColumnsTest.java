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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.apache.drill.categories.JdbcTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test compatibility with older versions of the server
 */
@Category(JdbcTest.class)
public class LegacyDatabaseMetaDataGetColumnsTest extends DatabaseMetaDataGetColumnsTest {

  @BeforeClass
  public static void setUpConnection() throws Exception {
    // Get JDBC connection to Drill:
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    Properties defaultProperties = getDefaultProperties();
    defaultProperties.setProperty("server.metadata.disabled", "true");

    connection = connect("jdbc:drill:zk=local", defaultProperties);
    dbMetadata = connection.getMetaData();

    DatabaseMetaDataGetColumnsTest.setUpMetadataToCheck();
  }


  // Override because of DRILL-1959

  @Override
  @Test
  public void test_SOURCE_DATA_TYPE_hasRightTypeString() throws SQLException {
    assertThat( rowsMetadata.getColumnTypeName( 22 ), equalTo( "INTEGER" ) );
  }

  @Override
  @Test
  public void test_SOURCE_DATA_TYPE_hasRightTypeCode() throws SQLException {
    assertThat( rowsMetadata.getColumnType( 22 ), equalTo( Types.INTEGER ) );
  }

  @Override
  @Test
  public void test_SOURCE_DATA_TYPE_hasRightClass() throws SQLException {
    assertThat( rowsMetadata.getColumnClassName( 22 ),
                equalTo( Integer.class.getName() ) );
  }
}
