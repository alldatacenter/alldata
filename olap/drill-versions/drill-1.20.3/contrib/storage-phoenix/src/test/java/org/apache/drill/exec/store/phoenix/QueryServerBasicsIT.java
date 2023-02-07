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
package org.apache.drill.exec.store.phoenix;

import static org.apache.phoenix.jdbc.PhoenixDatabaseMetaData.TABLE_CAT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.phoenix.end2end.QueryServerThread;
import org.apache.phoenix.query.BaseTest;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.apache.phoenix.util.ReadOnlyProps;
import org.apache.phoenix.util.ThinClientUtil;
import org.slf4j.LoggerFactory;

/**
 * This is a copy of {@link org.apache.phoenix.end2end.QueryServerBasicsIT} until
 * <a href="https://issues.apache.org/jira/browse/PHOENIX-6613">PHOENIX-6613</a> is fixed
 */
public class QueryServerBasicsIT extends BaseTest {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(QueryServerBasicsIT.class);

  private static QueryServerThread AVATICA_SERVER;
  private static Configuration CONF;
  protected static String CONN_STRING;

  public static synchronized void doSetup() throws Exception {
      setUpTestDriver(ReadOnlyProps.EMPTY_PROPS);

      CONF = config;
      if(System.getProperty("do.not.randomize.pqs.port") == null) {
        CONF.setInt(QueryServerProperties.QUERY_SERVER_HTTP_PORT_ATTRIB, 0);
      }
      String url = getUrl();
      AVATICA_SERVER = new QueryServerThread(new String[] { url }, CONF, QueryServerBasicsIT.class.getName());
      AVATICA_SERVER.start();
      AVATICA_SERVER.getQueryServer().awaitRunning();
      final int port = AVATICA_SERVER.getQueryServer().getPort();
      logger.info("Avatica server started on port " + port);
      CONN_STRING = ThinClientUtil.getConnectionUrl("localhost", port);
      logger.info("JDBC connection string is " + CONN_STRING);
  }

  public static void testCatalogs() throws Exception {
    try (final Connection connection = DriverManager.getConnection(CONN_STRING)) {
      assertFalse(connection.isClosed());
      try (final ResultSet resultSet = connection.getMetaData().getCatalogs()) {
        final ResultSetMetaData metaData = resultSet.getMetaData();
        assertFalse("unexpected populated resultSet", resultSet.next());
        assertEquals(1, metaData.getColumnCount());
        assertEquals(TABLE_CAT, metaData.getColumnName(1));
      }
    }
  }

  public static synchronized void afterClass() throws Exception {
    if (AVATICA_SERVER != null) {
      AVATICA_SERVER.join(TimeUnit.SECONDS.toSeconds(3));
      Throwable t = AVATICA_SERVER.getQueryServer().getThrowable();
      if (t != null) {
        fail("query server threw. " + t.getMessage());
      }
      assertEquals("query server didn't exit cleanly", 0, AVATICA_SERVER.getQueryServer().getRetCode());
    }
  }
}
