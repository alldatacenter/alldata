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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.junit.experimental.categories.Category;

@Category(JdbcTest.class)
public class Bug1735ResultSetCloseReleasesBuffersTest extends JdbcTestQueryBase {

  // TODO: Move Jetty status server disabling to DrillTest.

  private static final String STATUS_SERVER_PROPERTY_NAME =
      ExecConstants.HTTP_ENABLE;

  private static final String origStatusServerPropValue =
      System.getProperty( STATUS_SERVER_PROPERTY_NAME, "true" );

  // Disable Jetty status server so unit tests run (outside Maven setup).
  // (TODO:  Move this to base test class and/or have Jetty try other ports.)
  @BeforeClass
  public static void setUpClass() {
    System.setProperty( STATUS_SERVER_PROPERTY_NAME, "false" );
  }

  @AfterClass
  public static void tearDownClass() {
    System.setProperty( STATUS_SERVER_PROPERTY_NAME, origStatusServerPropValue );
  }

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

  @Test
  public void test() throws Exception {
    withNoDefaultSchema()
    .withConnection(
        new Function<Connection, Void>() {
          public Void apply( Connection connection ) {
            try {
              Statement statement = connection.createStatement();
              ResultSet resultSet = statement.executeQuery( "USE dfs.tmp" );
              // TODO:  Purge nextUntilEnd(...) and calls when remaining fragment
              // race conditions are fixed (not just DRILL-2245 fixes).
              // resultSet.close( resultSet );
              statement.close();
              // connection.close() is in withConnection(...)
              return null;
            } catch ( SQLException e ) {
              throw new RuntimeException( e );
            }
          }
        });
  }


}
