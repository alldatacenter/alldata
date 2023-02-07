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
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.alias.AliasRegistry;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_1_PASSWORD;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2;
import static org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl.TEST_USER_2_PASSWORD;

public class TestAliasSystemTables extends ClusterTest {
  private static AliasRegistry storageAliasesRegistry;

  private static AliasRegistry tableAliasesRegistry;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    cluster = ClusterFixture.bareBuilder(dirTestWatcher)
      .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.IMPERSONATION_ENABLED, true)
      .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, UserAuthenticatorTestImpl.TYPE)
      .build();
    storageAliasesRegistry = cluster.drillbit().getContext().getAliasRegistryProvider().getStorageAliasesRegistry();
    tableAliasesRegistry = cluster.drillbit().getContext().getAliasRegistryProvider().getTableAliasesRegistry();
  }

  @Test
  public void testStorageAliasesTable() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_2)
        .property(DrillProperties.PASSWORD, TEST_USER_2_PASSWORD)
        .build();
      storageAliasesRegistry.createPublicAliases();
      storageAliasesRegistry.getPublicAliases().put("`foobar`", "`cp`", false);
      storageAliasesRegistry.getPublicAliases().put("`abc`", "`def`", false);
      storageAliasesRegistry.createUserAliases(TEST_USER_1);
      storageAliasesRegistry.getUserAliases(TEST_USER_1).put("`ghi`", "`jkl`", false);
      storageAliasesRegistry.createUserAliases(TEST_USER_2);
      storageAliasesRegistry.getUserAliases(TEST_USER_2).put("`mno`", "`pqr`", false);

      String sql = "SELECT * FROM sys.storage_aliases";
      client.testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("alias", "name", "user", "isPublic")
        .baselineValues("`foobar`", "`cp`", null, true)
        .baselineValues("`abc`", "`def`", null, true)
        .baselineValues("`ghi`", "`jkl`", TEST_USER_1, false)
        .baselineValues("`mno`", "`pqr`", TEST_USER_2, false)
        .go();
    } finally {
      storageAliasesRegistry.deletePublicAliases();
      storageAliasesRegistry.deleteUserAliases(TEST_USER_1);
      storageAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }

  @Test
  public void testTableAliasesTable() throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, TEST_USER_1)
        .property(DrillProperties.PASSWORD, TEST_USER_1_PASSWORD)
        .build();
      tableAliasesRegistry.createPublicAliases();
      tableAliasesRegistry.getPublicAliases().put("`t1`", "`cp`.`tpch/lineitem.parquet`", false);
      tableAliasesRegistry.getPublicAliases().put("`t2`", "`cp`.`tpch/lineitem.parquet`", false);
      tableAliasesRegistry.createUserAliases(TEST_USER_1);
      tableAliasesRegistry.getUserAliases(TEST_USER_1).put("`t3`", "`cp`.`tpch/lineitem.parquet`", false);
      tableAliasesRegistry.createUserAliases(TEST_USER_2);
      tableAliasesRegistry.getUserAliases(TEST_USER_2).put("`t3`", "`cp`.`tpch/lineitem.parquet`", false);

      String sql = "SELECT * FROM sys.table_aliases";
      client.testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("alias", "name", "user", "isPublic")
        .baselineValues("`t1`", "`cp`.`tpch/lineitem.parquet`", null, true)
        .baselineValues("`t2`", "`cp`.`tpch/lineitem.parquet`", null, true)
        .baselineValues("`t3`", "`cp`.`tpch/lineitem.parquet`", TEST_USER_1, false)
        .baselineValues("`t3`", "`cp`.`tpch/lineitem.parquet`", TEST_USER_2, false)
        .go();
    } finally {
      tableAliasesRegistry.deletePublicAliases();
      tableAliasesRegistry.deleteUserAliases(TEST_USER_1);
      tableAliasesRegistry.deleteUserAliases(TEST_USER_2);
    }
  }
}
