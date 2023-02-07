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
package org.apache.drill.alias;

import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.alias.AliasRegistry;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.hamcrest.MatcherAssert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_GROUP;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_USER;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_USER_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.PROCESS_USER;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.PROCESS_USER_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2_PASSWORD;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestAliasSubstitution extends ClusterTest {
  private static AliasRegistry storageAliasesRegistry;
  private static AliasRegistry tableAliasesRegistry;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    cluster = ClusterFixture.bareBuilder(dirTestWatcher)
      .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.IMPERSONATION_ENABLED, true)
      .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, UserAuthenticatorTestImpl.TYPE)
      .configProperty(DrillProperties.USER, PROCESS_USER)
      .configProperty(DrillProperties.PASSWORD, PROCESS_USER_PASSWORD)
      .build();

    ClientFixture admin = cluster.clientBuilder()
      .property(DrillProperties.USER, PROCESS_USER)
      .property(DrillProperties.PASSWORD, PROCESS_USER_PASSWORD)
      .build();
    admin.alterSystem(ExecConstants.ADMIN_USERS_KEY, ADMIN_USER + "," + PROCESS_USER);
    admin.alterSystem(ExecConstants.ADMIN_USER_GROUPS_KEY, ADMIN_GROUP);

    client = cluster.clientBuilder()
      .property(DrillProperties.USER, ADMIN_USER)
      .property(DrillProperties.PASSWORD, ADMIN_USER_PASSWORD)
      .build();
    storageAliasesRegistry = cluster.drillbit().getContext().getAliasRegistryProvider().getStorageAliasesRegistry();
    tableAliasesRegistry = cluster.drillbit().getContext().getAliasRegistryProvider().getTableAliasesRegistry();
  }

  @Test
  public void testStoragePublicAlias() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`foobar`", "`cp`", false);

      String sql = "SELECT * FROM foobar.`tpch/lineitem.parquet` limit 1";
      long recordCount = queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testUseStoragePublicAlias() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`foobar`", "`dfs`", false);

      String sql = "use foobar";
      long recordCount = queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testStoragePublicAliasWithRealNameClash() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`dfs`", "`cp`", false);

      String sql = "SELECT * FROM `dfs`.`tpch/lineitem.parquet` limit 1";
      long recordCount = queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testTablePublicAlias() throws Exception {
    try {
      tableAliasesRegistry.createPublicAliases();
      tableAliasesRegistry.getPublicAliases().put("`tpch_lineitem`", "`cp`.`tpch/lineitem.parquet`", false);

      String sql = "SELECT * FROM tpch_lineitem limit 1";
      long recordCount = queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      tableAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testSchemaAndTablePublicAliases() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`foobar`", "`cp`", false);
      tableAliasesRegistry.createPublicAliases();
      // table alias refers to plugin alias, not the actual plugin name
      tableAliasesRegistry.getPublicAliases().put("`tpch_lineitem`", "`foobar`.`tpch/lineitem.parquet`", false);

      String sql = "SELECT * FROM tpch_lineitem limit 1";
      long recordCount = queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      tableAliasesRegistry.deletePublicAliases();
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDisableStoragePublicAlias() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`foobar`", "`cp`", false);
      client.alterSystem(ExecConstants.ENABLE_ALIASES, false);

      String sql = "SELECT * FROM foobar.`tpch/lineitem.parquet` limit 1";
      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("VALIDATION ERROR: Schema [[foobar]] is not valid with respect to either root schema or current default schema"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
      client.resetSystem(ExecConstants.ENABLE_ALIASES);
    }
  }

  @Test
  public void testDisableTablePublicAlias() throws Exception {
    try {
      tableAliasesRegistry.createPublicAliases();
      tableAliasesRegistry.getPublicAliases().put("`tpch_lineitem`", "`cp`.`tpch/lineitem.parquet`", false);

      client.alterSystem(ExecConstants.ENABLE_ALIASES, false);

      String sql = "SELECT * FROM tpch_lineitem limit 1";
      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("Object 'tpch_lineitem' not found"));
    } finally {
      tableAliasesRegistry.deletePublicAliases();
      client.resetSystem(ExecConstants.ENABLE_ALIASES);
    }
  }

  @Test
  public void testStorageUserAlias() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      storageAliasesRegistry.createUserAliases(TEST_USER_2);
      storageAliasesRegistry.getUserAliases(TEST_USER_2).put("`foobar`", "`cp`", false);

      String sql = "SELECT * FROM foobar.`tpch/lineitem.parquet` limit 1";
      long recordCount = client.queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testNonSharingStorageUserAlias() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_1)
        .property(DrillProperties.PASSWORD, TEST_USER_1_PASSWORD)
        .build();

      storageAliasesRegistry.createUserAliases(TEST_USER_2);
      storageAliasesRegistry.getUserAliases(TEST_USER_2).put("`foobar`", "`cp`", false);

      String sql = "SELECT * FROM foobar.`tpch/lineitem.parquet` limit 1";
      client.queryBuilder().sql(sql).run().recordCount();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("VALIDATION ERROR: Schema [[foobar]] is not valid with respect to either root schema or current default schema."));
    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testStorageUserAliasFallbackToPublic() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      storageAliasesRegistry.createUserAliases(TEST_USER_2);
      storageAliasesRegistry.getUserAliases(TEST_USER_2).put("`foobar`", "`cp`", false);

      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`foobar1`", "`cp`", false);

      String sql = "SELECT * FROM foobar1.`tpch/lineitem.parquet` limit 1";
      long recordCount = client.queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testStorageUserAndPublicAliasPriority() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      storageAliasesRegistry.createUserAliases(TEST_USER_2);
      storageAliasesRegistry.getUserAliases(TEST_USER_2).put("`foobar`", "`cp`", false);

      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`foobar`", "`dfs`", false);

      // user alias wins
      String sql = "SELECT * FROM foobar.`tpch/lineitem.parquet` limit 1";
      long recordCount = client.queryBuilder().sql(sql).run().recordCount();
      assertEquals(1, recordCount);
    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
      storageAliasesRegistry.deletePublicAliases();
    }
  }
}
