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
package org.apache.drill.exec.impersonation;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.dotdrill.DotDrillType;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.user.security.testing.UserAuthenticatorTestImpl;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.test.UserExceptionMatcher;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@Category({SlowTest.class, SecurityTest.class})
public class TestInboundImpersonation extends BaseTestImpersonation {

  public static final String OWNER = org1Users[0];
  public static final String OWNER_PASSWORD = "owner";

  public static final String TARGET_NAME = org1Users[1];
  public static final String TARGET_PASSWORD = "target";

  public static final String DATA_GROUP = org1Groups[0];

  public static final String PROXY_NAME = org1Users[2];
  public static final String PROXY_PASSWORD = "proxy";

  @BeforeClass
  public static void setup() throws Exception {
    startMiniDfsCluster(TestInboundImpersonation.class.getSimpleName());
    final DrillConfig newConfig = new DrillConfig(DrillConfig.create(cloneDefaultTestConfigProperties())
        .withValue(ExecConstants.USER_AUTHENTICATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true))
        .withValue(ExecConstants.USER_AUTHENTICATOR_IMPL,
            ConfigValueFactory.fromAnyRef(UserAuthenticatorTestImpl.TYPE))
        .withValue(ExecConstants.IMPERSONATION_ENABLED,
            ConfigValueFactory.fromAnyRef(true)));

    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, "anonymous");
    connectionProps.setProperty(DrillProperties.PASSWORD, "anything works!");
    updateTestCluster(1, newConfig, connectionProps);
    addMiniDfsBasedStorage(createTestWorkspaces());
    createTestData();
  }

  private static Map<String, WorkspaceConfig> createTestWorkspaces() throws Exception {
    Map<String, WorkspaceConfig> workspaces = Maps.newHashMap();
    createAndAddWorkspace(OWNER, getUserHome(OWNER), (short) 0755, OWNER, DATA_GROUP, workspaces);
    createAndAddWorkspace(PROXY_NAME, getUserHome(PROXY_NAME), (short) 0755, PROXY_NAME, DATA_GROUP,
        workspaces);
    return workspaces;
  }

  private static void createTestData() throws Exception {
    // Create table accessible only by OWNER
    final String tableName = "lineitem";
    updateClient(OWNER, OWNER_PASSWORD);
    test("USE " + getWSSchema(OWNER));
    test("CREATE TABLE %s as SELECT * FROM cp.`tpch/%s.parquet`", tableName, tableName);

    // Change the ownership and permissions manually.
    // Currently there is no option to specify the default permissions and ownership for new tables.
    final Path tablePath = new Path(getUserHome(OWNER), tableName);
    fs.setOwner(tablePath, OWNER, DATA_GROUP);
    fs.setPermission(tablePath, new FsPermission((short) 0700));

    // Create a view on top of lineitem table; allow IMPERSONATION_TARGET to read the view
    // /user/user0_1    u0_lineitem    750    user0_1:group0_1
    final String viewName = "u0_lineitem";
    test("ALTER SESSION SET `%s`='%o';", ExecConstants.NEW_VIEW_DEFAULT_PERMS_KEY, (short) 0750);
    test("CREATE VIEW %s.%s AS SELECT l_orderkey, l_partkey FROM %s.%s",
        getWSSchema(OWNER), viewName, getWSSchema(OWNER), "lineitem");
    // Verify the view file created has the expected permissions and ownership
    final Path viewFilePath = new Path(getUserHome(OWNER), viewName + DotDrillType.VIEW.getEnding());
    final FileStatus status = fs.getFileStatus(viewFilePath);
    assertEquals(org1Groups[0], status.getGroup());
    assertEquals(OWNER, status.getOwner());
    assertEquals((short) 0750, status.getPermission().toShort());

    // Authorize PROXY_NAME to impersonate TARGET_NAME
    updateClient(UserAuthenticatorTestImpl.PROCESS_USER,
        UserAuthenticatorTestImpl.PROCESS_USER_PASSWORD);
    test("ALTER SYSTEM SET `%s`='%s'", ExecConstants.IMPERSONATION_POLICIES_KEY,
        "[ { proxy_principals : { users: [\"" + PROXY_NAME + "\" ] },"
            + "target_principals : { users : [\"" + TARGET_NAME + "\"] } } ]");
  }

  @AfterClass
  public static void tearDown() throws Exception {
    updateClient(UserAuthenticatorTestImpl.PROCESS_USER,
        UserAuthenticatorTestImpl.PROCESS_USER_PASSWORD);
    test("ALTER SYSTEM RESET `%s`", ExecConstants.IMPERSONATION_POLICIES_KEY);
  }

  @Test
  public void selectChainedView() throws Exception {
    // Connect as PROXY_NAME and query for IMPERSONATION_TARGET
    // data belongs to OWNER, however a view is shared with IMPERSONATION_TARGET
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, PROXY_NAME);
    connectionProps.setProperty(DrillProperties.PASSWORD, PROXY_PASSWORD);
    connectionProps.setProperty(DrillProperties.IMPERSONATION_TARGET, TARGET_NAME);
    updateClient(connectionProps);

    testBuilder()
        .sqlQuery("SELECT * FROM %s.u0_lineitem ORDER BY l_orderkey LIMIT 1", getWSSchema(OWNER))
        .ordered()
        .baselineColumns("l_orderkey", "l_partkey")
        .baselineValues(1, 1552)
        .go();
  }

  @Test(expected = RpcException.class)
  // PERMISSION ERROR: Proxy user 'user2_1' is not authorized to impersonate target user 'user0_2'.
  public void unauthorizedTarget() throws Exception {
    final String unauthorizedTarget = org2Users[0];
    final Properties connectionProps = new Properties();
    connectionProps.setProperty(DrillProperties.USER, PROXY_NAME);
    connectionProps.setProperty(DrillProperties.PASSWORD, PROXY_PASSWORD);
    connectionProps.setProperty(DrillProperties.IMPERSONATION_TARGET, unauthorizedTarget);
    updateClient(connectionProps); // throws up
  }

  @Test
  public void invalidPolicy() throws Exception {
    thrownException.expect(new UserExceptionMatcher(UserBitShared.DrillPBError.ErrorType.VALIDATION,
        "Invalid impersonation policies."));
    updateClient(UserAuthenticatorTestImpl.PROCESS_USER,
        UserAuthenticatorTestImpl.PROCESS_USER_PASSWORD);
    test("ALTER SYSTEM SET `%s`='%s'", ExecConstants.IMPERSONATION_POLICIES_KEY,
        "[ invalid json ]");
  }

  @Test
  public void invalidProxy() throws Exception {
    thrownException.expect(new UserExceptionMatcher(UserBitShared.DrillPBError.ErrorType.VALIDATION,
        "Proxy principals cannot have a wildcard entry."));
    updateClient(UserAuthenticatorTestImpl.PROCESS_USER,
        UserAuthenticatorTestImpl.PROCESS_USER_PASSWORD);
    test("ALTER SYSTEM SET `%s`='%s'", ExecConstants.IMPERSONATION_POLICIES_KEY,
        "[ { proxy_principals : { users: [\"*\" ] },"
            + "target_principals : { users : [\"" + TARGET_NAME + "\"] } } ]");
  }
}
