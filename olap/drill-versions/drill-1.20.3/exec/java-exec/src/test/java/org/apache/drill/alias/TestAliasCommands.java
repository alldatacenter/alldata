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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_GROUP;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_USER;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.ADMIN_USER_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.PROCESS_USER;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.PROCESS_USER_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2_PASSWORD;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestAliasCommands extends ClusterTest {
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
  public void testCreateStoragePublicAlias() throws Exception {
    try {
      String sql = "CREATE PUBLIC ALIAS abc for storage dfs";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' for '`dfs`' created successfully")
        .go();

      List<String> expectedAliases = Collections.singletonList("`abc`");
      List<String> expectedValues = Collections.singletonList("`dfs`");

      List<String> actualAliases = new ArrayList<>();
      List<String> actualValues = new ArrayList<>();
      storageAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> {
        actualAliases.add(entry.getKey());
        actualValues.add(entry.getValue());
      });

      assertEquals(expectedAliases, actualAliases);
      assertEquals(expectedValues, actualValues);

    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testCreateTablePublicAlias() throws Exception {
    try {
      String sql = "CREATE PUBLIC ALIAS tpch_lineitem for table cp.`tpch/lineitem.parquet`";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Table alias '`tpch_lineitem`' for '`cp`.`default`.`tpch/lineitem.parquet`' created successfully")
        .go();

      List<String> expectedAliases = Collections.singletonList("`tpch_lineitem`");
      List<String> expectedValues = Collections.singletonList("`cp`.`default`.`tpch/lineitem.parquet`");

      List<String> actualAliases = new ArrayList<>();
      List<String> actualValues = new ArrayList<>();
      tableAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> {
        actualAliases.add(entry.getKey());
        actualValues.add(entry.getValue());
      });

      assertEquals(expectedAliases, actualAliases);
      assertEquals(expectedValues, actualValues);

    } finally {
      tableAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testCreateDuplicatedStoragePublicAlias() throws Exception {
    try {
      String sql = "CREATE PUBLIC ALIAS abc for storage dfs";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' for '`dfs`' created successfully")
        .go();

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("VALIDATION ERROR: Alias with given name [`abc`] already exists"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testCreateOrReplaceStoragePublicAlias() throws Exception {
    try {
      String sql = "CREATE OR REPLACE PUBLIC ALIAS abc for storage dfs";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' for '`dfs`' created successfully")
        .go();

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' for '`dfs`' created successfully")
        .go();

      List<String> expectedAliases = Collections.singletonList("`abc`");
      List<String> expectedValues = Collections.singletonList("`dfs`");

      List<String> actualAliases = new ArrayList<>();
      List<String> actualValues = new ArrayList<>();
      storageAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> {
        actualAliases.add(entry.getKey());
        actualValues.add(entry.getValue());
      });

      assertEquals(expectedAliases, actualAliases);
      assertEquals(expectedValues, actualValues);

    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testCreateAliasWithDisabledSupport() throws Exception {
    try {
      client.alterSystem(ExecConstants.ENABLE_ALIASES, false);
      String sql = "CREATE OR REPLACE PUBLIC ALIAS abc for storage dfs";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("Aliases support is disabled."));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
      client.resetSystem(ExecConstants.ENABLE_ALIASES);
    }
  }

  @Test
  public void testCreatePublicAliasWithNonAdminUser() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      String sql = "CREATE OR REPLACE PUBLIC ALIAS abc for storage dfs";

      client.queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("PERMISSION ERROR: Not authorized to perform operations on public aliases"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testCreateUserStorageAliasWithPublic() throws Exception {
    try {
      String sql = "CREATE PUBLIC ALIAS abc for storage dfs AS USER 'user1'";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("VALIDATION ERROR: Cannot create public alias for specific user"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testCreatePublicAliasForNonExistingStorage() throws Exception {
    try {
      String sql = "CREATE PUBLIC ALIAS abc for storage non_existing_storage";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("VALIDATION ERROR: Schema [non_existing_storage] is not valid " +
          "with respect to either root schema or current default schema"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testCreatePublicAliasForNonExistingTable() throws Exception {
    try {
      String sql = "CREATE PUBLIC ALIAS abc FOR TABLE cp.`tpch/non_existing_table.parquet`";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("VALIDATION ERROR: No table with given name " +
          "[tpch/non_existing_table.parquet] exists in schema [cp.default]"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropStoragePublicAlias() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`abc`", "`cp`", true);
      String sql = "DROP PUBLIC ALIAS abc FOR STORAGE";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' dropped successfully")
        .go();

      storageAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> fail());

    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropTablePublicAlias() throws Exception {
    try {
      tableAliasesRegistry.createPublicAliases();
      tableAliasesRegistry.getPublicAliases().put("`tpch_lineitem`", "`cp`.`default`.`tpch/lineitem.parquet`", true);
      String sql = "DROP PUBLIC ALIAS tpch_lineitem FOR TABLE";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Table alias '`tpch_lineitem`' dropped successfully")
        .go();

      tableAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> fail());

    } finally {
      tableAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropStoragePublicAliasIfExists() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`abc`", "`cp`", true);
      String sql = "DROP PUBLIC ALIAS IF EXISTS abc FOR STORAGE";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' dropped successfully")
        .go();

      storageAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> fail());

    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropAbsentStoragePublicAlias() throws Exception {
    try {
      String sql = "DROP PUBLIC ALIAS abc FOR STORAGE";

      queryBuilder().sql(sql).run();

      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("VALIDATION ERROR: No alias found with given name [`abc`]"));
    }
  }

  @Test
  public void testDropAbsentStoragePublicAliasIfExists() throws Exception {
    String sql = "DROP PUBLIC ALIAS IF EXISTS abc FOR STORAGE";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("ok", "summary")
      .baselineValues(false, "No storage alias found with given name [`abc`]")
      .go();

    storageAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> fail());
  }

  @Test
  public void testDropUserStorageAliasWithPublic() throws Exception {
    try {
      String sql = "DROP PUBLIC ALIAS IF EXISTS abc FOR STORAGE AS USER 'user1'";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("VALIDATION ERROR: Cannot drop public alias for specific user"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropAliasWithDisabledSupport() throws Exception {
    try {
      client.alterSystem(ExecConstants.ENABLE_ALIASES, false);
      String sql = "DROP PUBLIC ALIAS abc for storage";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("Aliases support is disabled."));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
      client.resetSystem(ExecConstants.ENABLE_ALIASES);
    }
  }

  @Test
  public void testDropPublicAliasWithNonAdminUser() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      String sql = "DROP PUBLIC ALIAS abc for storage";

      client.queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("PERMISSION ERROR: Not authorized to perform operations on public aliases."));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropAllStoragePublicAliases() throws Exception {
    try {
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`abc`", "`cp`", true);
      String sql = "DROP ALL PUBLIC ALIASES FOR STORAGE";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage aliases dropped successfully")
        .go();

      storageAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> fail());

    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropAllTablePublicAliases() throws Exception {
    try {
      tableAliasesRegistry.createPublicAliases();
      tableAliasesRegistry.getPublicAliases().put("`tpch_lineitem`", "`cp`.`default`.`tpch/lineitem.parquet`", true);
      String sql = "DROP ALL PUBLIC ALIASES FOR TABLE";

      testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Table aliases dropped successfully")
        .go();

      tableAliasesRegistry.getPublicAliases().getAllAliases().forEachRemaining(entry -> fail());

    } finally {
      tableAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropAllUserStorageAliasWithPublic() throws Exception {
    try {
      String sql = "DROP ALL PUBLIC ALIASES FOR STORAGE AS USER 'user1'";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("VALIDATION ERROR: Cannot drop public aliases for specific user"));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
    }
  }

  @Test
  public void testDropAllAliasesWithDisabledSupport() throws Exception {
    try {
      client.alterSystem(ExecConstants.ENABLE_ALIASES, false);
      String sql = "DROP ALL PUBLIC ALIASES for storage";

      queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(), containsString("Aliases support is disabled."));
    } finally {
      storageAliasesRegistry.deletePublicAliases();
      client.resetSystem(ExecConstants.ENABLE_ALIASES);
    }
  }

  @Test
  public void testDropAllPublicAliasesWithNonAdminUser() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      String sql = "DROP ALL PUBLIC ALIASES for storage";

      client.queryBuilder().sql(sql).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("PERMISSION ERROR: Not authorized to perform operations on public aliases."));
    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testCreateStorageUserAlias() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      String sql = "CREATE ALIAS abc for storage dfs";

      client.testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' for '`dfs`' created successfully")
        .go();

      List<String> expectedAliases = Collections.singletonList("`abc`");
      List<String> expectedValues = Collections.singletonList("`dfs`");

      List<String> actualAliases = new ArrayList<>();
      List<String> actualValues = new ArrayList<>();
      storageAliasesRegistry.getUserAliases(TEST_USER_2).getAllAliases().forEachRemaining(entry -> {
        actualAliases.add(entry.getKey());
        actualValues.add(entry.getValue());
      });

      assertEquals(expectedAliases, actualAliases);
      assertEquals(expectedValues, actualValues);

    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testCreateTableUserAlias() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      String sql = "CREATE ALIAS tpch_lineitem for table cp.`tpch/lineitem.parquet`";

      client.testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Table alias '`tpch_lineitem`' for '`cp`.`default`.`tpch/lineitem.parquet`' created successfully")
        .go();

      List<String> expectedAliases = Collections.singletonList("`tpch_lineitem`");
      List<String> expectedValues = Collections.singletonList("`cp`.`default`.`tpch/lineitem.parquet`");

      List<String> actualAliases = new ArrayList<>();
      List<String> actualValues = new ArrayList<>();
      tableAliasesRegistry.getUserAliases(TEST_USER_2).getAllAliases().forEachRemaining(entry -> {
        actualAliases.add(entry.getKey());
        actualValues.add(entry.getValue());
      });

      assertEquals(expectedAliases, actualAliases);
      assertEquals(expectedValues, actualValues);

    } finally {
      tableAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testDropStorageUserAlias() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      storageAliasesRegistry.createUserAliases(TEST_USER_2);
      storageAliasesRegistry.getUserAliases(TEST_USER_2).put("`abc`", "`cp`", true);
      String sql = "DROP ALIAS abc FOR STORAGE";

      client.testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Storage alias '`abc`' dropped successfully")
        .go();

      storageAliasesRegistry.getUserAliases(TEST_USER_2).getAllAliases().forEachRemaining(entry -> fail());

    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testDropAllTableUserAliases() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      tableAliasesRegistry.createUserAliases(TEST_USER_2);
      tableAliasesRegistry.getUserAliases(TEST_USER_2).put("`tpch_lineitem`", "`cp`.`default`.`tpch/lineitem.parquet`", true);
      String sql = "DROP ALL ALIASES FOR TABLE";

      client.testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Table aliases dropped successfully")
        .go();

      tableAliasesRegistry.getUserAliases(TEST_USER_2).getAllAliases().forEachRemaining(entry -> fail());

    } finally {
      tableAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testCreateTableAliasAsTheSameUser() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      String sql = "CREATE ALIAS tpch_lineitem for table cp.`tpch/lineitem.parquet` AS USER '%s'";

      client.testBuilder()
        .sqlQuery(sql, TEST_USER_2)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Table alias '`tpch_lineitem`' for '`cp`.`default`.`tpch/lineitem.parquet`' created successfully")
        .go();

      List<String> expectedAliases = Collections.singletonList("`tpch_lineitem`");
      List<String> expectedValues = Collections.singletonList("`cp`.`default`.`tpch/lineitem.parquet`");

      List<String> actualAliases = new ArrayList<>();
      List<String> actualValues = new ArrayList<>();
      tableAliasesRegistry.getUserAliases(TEST_USER_2).getAllAliases().forEachRemaining(entry -> {
        actualAliases.add(entry.getKey());
        actualValues.add(entry.getValue());
      });

      assertEquals(expectedAliases, actualAliases);
      assertEquals(expectedValues, actualValues);

    } finally {
      tableAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testAdminCreateTableAliasAsAnotherUser() throws Exception {
    try {
      String sql = "CREATE ALIAS tpch_lineitem for table cp.`tpch/lineitem.parquet` AS USER '%s'";

      testBuilder()
        .sqlQuery(sql, TEST_USER_2)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, "Table alias '`tpch_lineitem`' for '`cp`.`default`.`tpch/lineitem.parquet`' created successfully")
        .go();

      List<String> expectedAliases = Collections.singletonList("`tpch_lineitem`");
      List<String> expectedValues = Collections.singletonList("`cp`.`default`.`tpch/lineitem.parquet`");

      List<String> actualAliases = new ArrayList<>();
      List<String> actualValues = new ArrayList<>();
      tableAliasesRegistry.getUserAliases(TEST_USER_2).getAllAliases().forEachRemaining(entry -> {
        actualAliases.add(entry.getKey());
        actualValues.add(entry.getValue());
      });

      assertEquals(expectedAliases, actualAliases);
      assertEquals(expectedValues, actualValues);

    } finally {
      tableAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testNonAdminCreateTableAliasAsAnotherUser() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();

      String sql = "CREATE ALIAS tpch_lineitem for table cp.`tpch/lineitem.parquet` AS USER '%s'";

      client.queryBuilder().sql(sql, TEST_USER_1).run();
      fail();
    } catch (UserRemoteException e) {
      MatcherAssert.assertThat(e.getVerboseMessage(),
        containsString("PERMISSION ERROR: Not authorized to perform operations on aliases for other users."));
    } finally {
      storageAliasesRegistry.deleteUserAliases(TEST_USER_1);
    }
  }
}
