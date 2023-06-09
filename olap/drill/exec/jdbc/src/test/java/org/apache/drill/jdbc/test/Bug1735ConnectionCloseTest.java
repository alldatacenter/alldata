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

import org.apache.drill.test.TestTools;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
* Test for DRILL-1735:  Closing local JDBC connection didn't shut down
* local DrillBit to free resources (plus QueryResultBatch buffer allocation leak
* in DrillCursor.next(), lack of DrillMetrics reset, vectors buffer leak under
* DrillCursor/DrillResultSet, and other problems).
*/
@Category(JdbcTest.class)
public class Bug1735ConnectionCloseTest extends JdbcTestQueryBase {

  static final Logger logger = getLogger( Bug1735ConnectionCloseTest.class );

  @Rule
  public TestRule TIMEOUT = TestTools.getTimeoutRule( 120_000 /* ms */ );

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


  // Basic sanity test (too small to detect original connection close problem
  // but would detect QueryResultBatch release and metrics problems).

  private static final int SMALL_ITERATION_COUNT = 3;

  @Test
  public void testCloseDoesntLeakResourcesBasic() throws Exception {
    for ( int i = 1; i <= SMALL_ITERATION_COUNT; i++ ) {
      Connection connection = new Driver().connect("jdbc:drill:zk=local", getDefaultProperties());
      connection.close();
    }
  }


  // Test large enough to detect connection close problem (at least on
  // developer's machine).

  private static final int LARGE_ITERATION_COUNT = 1000;

  @Ignore( "Normally suppressed because slow" )
  @Test
  public void testCloseDoesntLeakResourcesMany() throws Exception {
    for ( int i = 1; i <= LARGE_ITERATION_COUNT; i++ ) {
      // (Note: Can't use JdbcTest's connect(...) because it returns connection
      // that doesn't really close.
      Connection connection = new Driver().connect("jdbc:drill:zk=local", getDefaultProperties());
      connection.close();
    }
  }

}
