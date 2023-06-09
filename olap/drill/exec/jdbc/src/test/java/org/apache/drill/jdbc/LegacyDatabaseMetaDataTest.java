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

import java.sql.SQLException;
import java.util.Properties;

import org.apache.drill.categories.JdbcTest;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

/**
 * Test compatibility with older versions of the server
 */
@Category(JdbcTest.class)
public class LegacyDatabaseMetaDataTest extends DatabaseMetaDataTest {
  @BeforeClass
  public static void setUpConnection() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("server.metadata.disabled", "true");
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = new Driver().connect( "jdbc:drill:zk=local", properties );
    dbmd = connection.getMetaData();
  }
}
