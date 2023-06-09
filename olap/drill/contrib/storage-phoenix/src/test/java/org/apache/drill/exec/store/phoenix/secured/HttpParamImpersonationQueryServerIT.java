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
package org.apache.drill.exec.store.phoenix.secured;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.coprocessor.CoprocessorHost;
import org.apache.hadoop.hbase.security.access.AccessControlClient;
import org.apache.hadoop.hbase.security.access.AccessController;
import org.apache.hadoop.hbase.security.access.Permission.Action;
import org.apache.hadoop.hbase.security.token.TokenProvider;
import org.apache.phoenix.end2end.NeedsOwnMiniClusterTest;
import org.apache.phoenix.end2end.TlsUtil;
import org.apache.phoenix.jdbc.PhoenixDatabaseMetaData;
import org.apache.phoenix.queryserver.QueryServerOptions;
import org.apache.phoenix.queryserver.QueryServerProperties;
import org.apache.phoenix.queryserver.client.Driver;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;

import static org.apache.drill.exec.store.phoenix.secured.QueryServerEnvironment.LOGIN_USER;

/**
 * This is a copy of {@link org.apache.phoenix.end2end.HttpParamImpersonationQueryServerIT},
 * but customized with 3 users, see {@link SecuredPhoenixBaseTest#runForThreeClients} for details
 */
@Category(NeedsOwnMiniClusterTest.class)
public class HttpParamImpersonationQueryServerIT {

  public static QueryServerEnvironment environment;

  private static final List<TableName> SYSTEM_TABLE_NAMES = Arrays.asList(
    PhoenixDatabaseMetaData.SYSTEM_CATALOG_HBASE_TABLE_NAME,
    PhoenixDatabaseMetaData.SYSTEM_MUTEX_HBASE_TABLE_NAME,
    PhoenixDatabaseMetaData.SYSTEM_FUNCTION_HBASE_TABLE_NAME,
    PhoenixDatabaseMetaData.SYSTEM_SCHEMA_HBASE_TABLE_NAME,
    PhoenixDatabaseMetaData.SYSTEM_SEQUENCE_HBASE_TABLE_NAME,
    PhoenixDatabaseMetaData.SYSTEM_STATS_HBASE_TABLE_NAME);

  public static synchronized void startQueryServerEnvironment() throws Exception {
    // Clean up previous environment if any (Junit 4.13 @BeforeParam / @AfterParam would be an alternative)
    if(environment != null) {
      stopEnvironment();
    }

    final Configuration conf = new Configuration();
    conf.setStrings(CoprocessorHost.MASTER_COPROCESSOR_CONF_KEY, AccessController.class.getName());
    conf.setStrings(CoprocessorHost.REGIONSERVER_COPROCESSOR_CONF_KEY, AccessController.class.getName());
    conf.setStrings(CoprocessorHost.REGION_COPROCESSOR_CONF_KEY, AccessController.class.getName(), TokenProvider.class.getName());

    // Set the proxyuser settings,
    // so that the user who is running the Drillbits/MiniDfs can impersonate user1 and user2 (not user3)
    conf.set(String.format("hadoop.proxyuser.%s.hosts", LOGIN_USER), "*");
    conf.set(String.format("hadoop.proxyuser.%s.users", LOGIN_USER), "user1,user2");
    conf.setBoolean(QueryServerProperties.QUERY_SERVER_WITH_REMOTEUSEREXTRACTOR_ATTRIB, true);
    environment = new QueryServerEnvironment(conf, 3, false);
  }

  public static synchronized void stopEnvironment() throws Exception {
    environment.stop();
    environment = null;
  }

  static public String getUrlTemplate() {
    String url = Driver.CONNECT_STRING_PREFIX + "url=%s://localhost:" + environment.getPqsPort() + "?"
      + QueryServerOptions.DEFAULT_QUERY_SERVER_REMOTEUSEREXTRACTOR_PARAM + "=%s;authentication=SPNEGO;serialization=PROTOBUF%s";
    if (environment.getTls()) {
      return String.format(url, "https", "%s", ";truststore=" + TlsUtil.getTrustStoreFile().getAbsolutePath()
        + ";truststore_password=" + TlsUtil.getTrustStorePassword());
    } else {
      return String.format(url, "http", "%s", "");
    }
  }

  static void grantUsersToPhoenixSystemTables(List<String> usersToGrant) throws Exception {
    // Grant permission to the user to access the system tables
    try {
      for (String user : usersToGrant) {
        for (TableName tn : SYSTEM_TABLE_NAMES) {
          AccessControlClient.grant(environment.getUtil().getConnection(), tn, user, null, null, Action.READ, Action.EXEC);
        }
      }
    } catch (Throwable e) {
      throw new Exception(e);
    }
  }

  static void grantUsersToGlobalPhoenixUserTables(List<String> usersToGrant) throws Exception {
    // Grant permission to the user to access the user tables
    try {
      for (String user : usersToGrant) {
        AccessControlClient.grant(environment.getUtil().getConnection(), user, Action.READ, Action.EXEC);
      }
    } catch (Throwable e) {
      throw new Exception(e);
    }
  }
}
